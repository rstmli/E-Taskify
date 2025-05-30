package az.etaskify.service;

import az.etaskify.client.AuthClient;
import az.etaskify.dao.entity.InviteEntity;
import az.etaskify.dao.entity.JoinRequestEntity;
import az.etaskify.dao.entity.OrganizationEntity;
import az.etaskify.dao.entity.UserOrganizationEntity;
import az.etaskify.dao.repository.InviteRepository;
import az.etaskify.dao.repository.JoinRequestRepository;
import az.etaskify.dao.repository.OrganizationRepository;
import az.etaskify.dao.repository.UserOrganizationRepository;
import az.etaskify.dto.*;
import az.etaskify.exception.*;
import az.etaskify.mapper.JoinRequestMapper;
import az.etaskify.mapper.OrganizationMapper;
import az.etaskify.util.enums.InviteStatus;
import az.etaskify.util.enums.UserRole;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.naming.ServiceUnavailableException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final AuthClient authClient;
    private final UserOrganizationRepository userOrganizationRepository;
    private final InviteRepository inviteRepository;
    private final AuthClient authServiceClient;
    private final NotificationService notificationService;
    private final JoinRequestRepository joinRequestRepository;
    private final JoinRequestMapper joinRequestMapper;

    public ResponseEntity<String> createOrganization(OrganizationCreateRequest dto, String authHeader) {
        try {
            var userId = authClient.validateToken(authHeader).getBody();
            var entity = organizationMapper.orgDtoToEntity(dto, userId);
            organizationRepository.save(entity);
            return ResponseEntity.ok("The organization was successfully created");
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("server error");
        }

    }

    @Transactional
    public InviteResponseDto inviteUserToOrganization(Long organizationId, InviteUserRequestDto requestDto,
                                                      String authHeader) throws ServiceUnavailableException {
        var userId = authClient.validateToken(authHeader).getBody();
        log.info("Attempting to invite user '{}' to organization ID: {} by user ID: {}", requestDto.username(),
                organizationId, userId);

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> {
                    log.error("Organization not found with ID: {}", organizationId);
                    return new OrganizationNotFoundException("Organization not found with ID: " + organizationId);
                });

        if (!organization.getIsPrivate()) {
            log.warn("Attempted to invite user to a public organization ID: {}", organizationId);
            throw new CannotInviteToPublicOrganizationException("Invitations can only be sent for private organizations.");
        }

        boolean isOwner = organization.getOwnerId().equals(userId);

        if (!isOwner) {
            throw new NotAuthorizedToInviteException("User is not authorized to send invitations for this organization.");
        }

        UserDto invitedUserDto;
        try {
            invitedUserDto = authServiceClient.findUserByUsername(requestDto.username());
            if (invitedUserDto == null || invitedUserDto.getId() == null) {
                throw new InvitedUserNotFoundException("Invited user not found: " + requestDto.username());
            }
        } catch (FeignException.NotFound e) {
            throw new InvitedUserNotFoundException("Invited user not found: " + requestDto.username());
        } catch (Exception e) {
            throw new ServiceUnavailableException("Could not reach authentication service to verify user.");
        }

        Long invitedUserId = invitedUserDto.getId();

        if (userId.equals(invitedUserId)) {
            throw new CannotInviteSelfException("You cannot invite yourself to the organization.");
        }

        if (userOrganizationRepository.existsByUserIdAndOrganizationId(invitedUserId, organizationId)) {
            throw new UserAlreadyMemberException("User '" + requestDto.username() + "' is already a member of this " +
                    "organization.");
        }
        if (inviteRepository.existsByOrganizationIdAndInvitedUserIdAndStatus(organizationId, invitedUserId,
                InviteStatus.PENDING)) {
            throw new InviteAlreadyPendingException("An invitation is already pending for user '" +
                    requestDto.username() + "'.");
        }

        InviteEntity newInvite = InviteEntity.builder()
                .organization(organization)
                .invitedUserId(invitedUserId)
                .inviterUserId(userId)
                .status(InviteStatus.PENDING)
                .build();

        InviteEntity savedInvite = inviteRepository.save(newInvite);

        log.info("Successfully created invite ID: {} for user ID: {} to organization ID: {}", savedInvite.getId(),
                invitedUserId, organizationId);

        try {
            notificationService.sendInviteNotificationAsync(invitedUserId, userId, organization.getName());
            log.info("Sent invite notification to user ID: {}", invitedUserId);
        } catch (Exception e) {
            log.error("Failed to send invite notification to user ID: {}. Error: {}", invitedUserId, e.getMessage(), e);
        }
        return organizationMapper.mapToInviteResponseDto(savedInvite);
    }

    public List<InviteResponseDto> listMyPendingInvites(String authHeader) {
        var userId = authClient.validateToken(authHeader).getBody();
        log.info("Fetching pending invites for user ID: {}", userId);
        List<InviteEntity> pendingInvites = inviteRepository.findByInvitedUserIdAndStatusOrderByCreatedAtDesc(userId,
                InviteStatus.PENDING);

        return organizationMapper.entityToDtoList(pendingInvites);
    }

    @Transactional
    public InviteResponseDto acceptInvite(Long inviteId, String authHeader) throws AccessDeniedException {
        var userId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} attempting to accept invite ID: {}", userId, inviteId);

        InviteEntity invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> {
                    getNotInviteLog(inviteId);
                    return new InviteNotFoundException("Invitation not found.");
                });

        if (!invite.getInvitedUserId().equals(userId)) {
            log.warn("Security violation: User ID {} tried to accept invite ID {} belonging to user ID {}",
                    userId, inviteId, invite.getInvitedUserId());
            throw new AccessDeniedException("You are not authorized to accept this invitation.");
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            unprocessableInviteLog(inviteId, invite);
            throw new InviteCannotBeProcessedException("This invitation cannot be processed (Status: " +
                    invite.getStatus() + ").");
        }

        OrganizationEntity organization = invite.getOrganization();

        if (userOrganizationRepository.existsByUserIdAndOrganizationId(userId, organization.getId())) {
            log.warn("User ID: {} is already a member of organization ID: {} but tried to accept invite ID: {}. " +
                            "Setting invite to accepted.",
                    userId, organization.getId(), inviteId);
            throw new UserAlreadyMemberException("You are already a member of this organization.");
        } else {
            UserOrganizationEntity membership = UserOrganizationEntity.builder()
                    .userId(userId)
                    .organization(organization)
                    .role(UserRole.MEMBER)
                    .build();
            userOrganizationRepository.save(membership);
            log.info("User ID: {} successfully added to organization ID: {} with role: {}", userId,
                    organization.getId(), UserRole.MEMBER);
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        InviteEntity updatedInvite = inviteRepository.save(invite);
        log.info("Invite ID: {} status updated to ACCEPTED", inviteId);

        try {
            notificationService.sendInviteAcceptedNotification(invite.getInviterUserId(), userId, organization.getName());
            log.info("Sent invite accepted notification to inviter ID: {}", invite.getInviterUserId());
        } catch (Exception e) {
            log.error("Failed to send invite accepted notification to inviter ID: {}. Error: {}",
                    invite.getInviterUserId(), e.getMessage(), e);
        }

        return organizationMapper.mapToInviteResponseDto(updatedInvite);
    }

    @Transactional
    public InviteResponseDto rejectInvite(Long inviteId, String authHeader) throws AccessDeniedException {
        var userId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} attempting to reject invite ID: {}", userId, inviteId);

        InviteEntity invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> {
                    getNotInviteLog(inviteId);
                    return new InviteNotFoundException("Invitation not found.");
                });

        if (!invite.getInvitedUserId().equals(userId)) {
            log.warn("Security violation: User ID {} tried to reject invite ID {} belonging to user ID {}",
                    userId, inviteId, invite.getInvitedUserId());
            throw new AccessDeniedException("You are not authorized to reject this invitation.");
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            unprocessableInviteLog(inviteId, invite);
            throw new InviteCannotBeProcessedException("This invitation cannot be processed (Status: " +
                    invite.getStatus() + ").");
        }

        invite.setStatus(InviteStatus.REJECTED);
        InviteEntity updatedInvite = inviteRepository.save(invite);
        log.info("Invite ID: {} status updated to REJECTED", inviteId);

        try {
            notificationService.sendInviteRejectedNotification(invite.getInviterUserId(), userId,
                    invite.getOrganization().getName());
            log.info("Sent invite rejected notification to inviter ID: {}", invite.getInviterUserId());
        } catch (Exception e) {
            log.error("Failed to send invite rejected notification to inviter ID: {}. Error: {}",
                    invite.getInviterUserId(), e.getMessage(), e);
        }

        return organizationMapper.mapToInviteResponseDto(updatedInvite);
    }

    public List<PublicOrganizationDto> findPublicOrganizations(String searchTerm) {
        List<OrganizationEntity> organizations;
        if (searchTerm != null && !searchTerm.isBlank()) {
            log.info("Searching for public organization with term: {}", searchTerm);
            organizations = organizationRepository.findByNameContainingIgnoreCaseAndIsPrivateFalse(searchTerm);
        } else {
            log.info("Listing all organizations");
            organizations = organizationRepository.findByIsPrivateFalse();
        }
        log.info("Found {} public organizations", organizations.size());
        return organizationMapper.mapToPublicOrganizationDtoList(organizations);
    }

    @Transactional
    public JoinRequestDto requestToJoinOrganization(Long organizationId, String authHeader) {
        var userId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} attempting to send join request to Organization ID: {}", userId, organizationId);
        OrganizationEntity organization = organizationRepository.findById(organizationId).orElseThrow(() -> {
            log.error("Organization not found with ID: {}", organizationId);
            return new OrganizationNotFoundException("Organization not found with ID: " + organizationId);
        });
        if (organization.getIsPrivate()) {
            log.warn("User ID: {} attempted to join private organization ID: {} via request.", userId, organizationId);
            throw new CannotRequestToJoinPrivateOrganizationException("You can only request to join public organizations.");
        }

        if (userOrganizationRepository.existsByUserIdAndOrganizationId(userId, organizationId)) {
            log.warn("User ID: {} is already a member of public Organization ID: {}", userId, organizationId);
            throw new UserAlreadyMemberException("You are already a member of this organization.");
        }

        if (joinRequestRepository.existsByOrganizationIdAndUserIdAndStatus(organizationId, userId, InviteStatus.PENDING)) {
            log.warn("User ID: {} already has a pending join request for Organization ID: {}", userId, organizationId);
            throw new JoinRequestAlreadyPendingException("You already have a pending request to join this organization");
        }

        JoinRequestEntity newRequest = JoinRequestEntity.builder()
                .userId(userId)
                .organization(organization)
                .status(InviteStatus.PENDING)
                .build();

        var savedRequest = joinRequestRepository.save(newRequest);
        log.info("Successfully created Join Request ID: {} for User ID: {} to organization ID: {}", savedRequest.getId(),
                userId, organizationId);
        try {
            notificationService.sendJoinRequestReceivedNotification(organization.getOwnerId(), userId,
                    organization.getName());
            log.info("Sent join request received notification to owner ID: {}", organization.getOwnerId());
        } catch (Exception e) {
            log.error("Failed to send join request received notification to owner ID: {}. Error: {}",
                    organization.getOwnerId(), e.getMessage(), e);
        }
        return joinRequestMapper.mapToJoinRequestDto(savedRequest);
    }

    public List<JoinRequestDto> listPendingJoinRequests(Long organizationId, String authHeader) {
        var userId = authClient.validateToken(authHeader).getBody();
        log.info("Owner ID: {} attempting to list pending join requests for Organization ID: {}", userId, organizationId);

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found with ID: " + organizationId));

        if (!organization.getOwnerId().equals(userId)) {
            log.warn("Unauthorized attempt by User ID: {} to list join requests for Organization ID: {}", userId, organizationId);
            throw new NotAuthorizedException("You are not authorized to view join requests for this organization.");
        }

        List<JoinRequestEntity> pendingRequests = joinRequestRepository.findByOrganizationIdAndStatus(organizationId, InviteStatus.PENDING);
        log.info("Found {} pending join requests for Organization ID: {}", pendingRequests.size(), organizationId);

        List<JoinRequestDto> dtos = pendingRequests.stream()
                .map(request -> {
                    JoinRequestDto dto = joinRequestMapper.mapToJoinRequestDto(request);
                    try {
                        UserDto userDto = authServiceClient.findUserById(request.getUserId());
                        dto.setRequestingUsername(userDto != null ? userDto.getUsername() : "Unknown User");
                    } catch (Exception e) {
                        log.error("Could not fetch username for user ID {} while listing join requests", request.getUserId(), e);
                        dto.setRequestingUsername("Error Fetching Name");
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return dtos;
    }

    @Transactional
    public JoinRequestDto approveJoinRequest(Long requestId, String authHeader) {
        var ownerId = authClient.validateToken(authHeader).getBody();
        log.info("Owner ID: {} attempting to approve Join Request ID: {}", ownerId, requestId);

        JoinRequestEntity request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> {
                    joinRequestNotFoundLog(requestId);
                    return new JoinRequestNotFoundException("Join request not found.");
                });

        OrganizationEntity organization = request.getOrganization();

        if (!organization.getOwnerId().equals(ownerId)) {
            log.warn("Unauthorized attempt by User ID: {} to approve Join Request ID: {}", ownerId, requestId);
            throw new NotAuthorizedException("You are not authorized to approve requests for this organization.");
        }

        if (request.getStatus() != InviteStatus.PENDING) {
            unprocessableJoinRequestLog(requestId,request);
            throw new JoinRequestCannotBeProcessedException("This request cannot be processed (Status: " + request.getStatus() + ").");
        }

        Long requestingUserId = request.getUserId();

        if (userOrganizationRepository.existsByUserIdAndOrganizationId(requestingUserId, organization.getId())) {
            log.warn("User ID: {} is already a member of Organization ID: {} but tried to approve join request ID: {}. Setting request to approved.",
                    requestingUserId, organization.getId(), requestId);
        } else {
            UserOrganizationEntity membership = UserOrganizationEntity.builder()
                    .userId(requestingUserId)
                    .organization(organization)
                    .role(UserRole.MEMBER)
                    .build();
            userOrganizationRepository.save(membership);
            log.info("User ID: {} successfully added to Organization ID: {} with role: {}", requestingUserId, organization.getId(), UserRole.MEMBER);
        }

        request.setStatus(InviteStatus.ACCEPTED);
        JoinRequestEntity updatedRequest = joinRequestRepository.save(request);
        log.info("Join Request ID: {} status updated to ACCEPTED", requestId);

        try {
            notificationService.sendJoinRequestApprovedNotification(requestingUserId, organization.getName());
            log.info("Sent join request approved notification to user ID: {}", requestingUserId);
        } catch (Exception e) {
            log.error("Failed to send join request approved notification to user ID: {}. Error: {}", requestingUserId, e.getMessage(), e);
        }

        return joinRequestMapper.mapToJoinRequestDto(updatedRequest);
    }

    @Transactional
    public JoinRequestDto rejectJoinRequest(Long requestId, String authHeader) {
        var ownerId = authClient.validateToken(authHeader).getBody();
        log.info("Owner ID: {} attempting to reject Join Request ID: {}", ownerId, requestId);

        JoinRequestEntity request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> {
                    joinRequestNotFoundLog(requestId);
                    return new JoinRequestNotFoundException("Join request not found.");
                });

        OrganizationEntity organization = request.getOrganization();

        if (!organization.getOwnerId().equals(ownerId)) {
            log.warn("Unauthorized attempt by User ID: {} to reject Join Request ID: {}", ownerId, requestId);
            throw new NotAuthorizedException("You are not authorized to reject requests for this organization.");
        }

        if (request.getStatus() != InviteStatus.PENDING) {
            unprocessableJoinRequestLog(requestId,request);
            throw new JoinRequestCannotBeProcessedException("This request cannot be processed (Status: " + request.getStatus() + ").");
        }

        Long requestingUserId = request.getUserId();

        request.setStatus(InviteStatus.REJECTED);
        JoinRequestEntity updatedRequest = joinRequestRepository.save(request);
        log.info("Join Request ID: {} status updated to REJECTED", requestId);

        try {
            notificationService.sendJoinRequestRejectedNotification(requestingUserId, organization.getName());
            log.info("Sent join request rejected notification to user ID: {}", requestingUserId);
        } catch (Exception e) {
            log.error("Failed to send join request rejected notification to user ID: {}. Error: {}", requestingUserId, e.getMessage(), e);
        }

        return joinRequestMapper.mapToJoinRequestDto(updatedRequest);
    }


    private void unprocessableInviteLog(Long inviteId, InviteEntity invite) {
        log.warn("Invite ID: {} cannot be processed because its status is {}", inviteId, invite.getStatus());
    }

    private void getNotInviteLog(Long inviteId) {
        log.error("Invite not found with ID: {}", inviteId);
    }

    private void unprocessableJoinRequestLog(Long requestId,JoinRequestEntity request){
        log.warn("Join Request ID: {} cannot be processed because its status is {}", requestId, request.getStatus());
    }
    private void joinRequestNotFoundLog(Long requestId){
        log.error("Join Request not found with ID: {}", requestId);
    }

}
