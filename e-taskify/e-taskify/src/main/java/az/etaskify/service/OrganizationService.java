package az.etaskify.service;

import az.etaskify.client.AuthClient;
import az.etaskify.dao.entity.InviteEntity;
import az.etaskify.dao.entity.OrganizationEntity;
import az.etaskify.dao.entity.UserOrganizationEntity;
import az.etaskify.dao.repository.InviteRepository;
import az.etaskify.dao.repository.OrganizationRepository;
import az.etaskify.dao.repository.UserOrganizationRepository;
import az.etaskify.dto.InviteResponseDto;
import az.etaskify.dto.InviteUserRequestDto;
import az.etaskify.dto.OrganizationCreateRequest;
import az.etaskify.dto.UserDto;
import az.etaskify.exception.*;
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

    @Transactional
    public ResponseEntity<String> createOrganization (OrganizationCreateRequest dto, String authHeader) {
        try{
            var userId = authClient.validateToken(authHeader).getBody();
            var entity = organizationMapper.orgDtoToEntity(dto,userId);
            organizationRepository.save(entity);
            UserOrganizationEntity membership = UserOrganizationEntity.builder()
                    .userId(userId)
                    .organization(entity)
                    .role(UserRole.ADMIN)
                    .build();
            userOrganizationRepository.save(membership);
            return ResponseEntity.ok("The organization was successfully created");
        } catch (RuntimeException e){
            return ResponseEntity.internalServerError().body("server error");
        }

    }
    @Transactional
    public InviteResponseDto inviteUserToOrganization(Long organizationId, InviteUserRequestDto requestDto, String authHeader) throws ServiceUnavailableException {
        var userId = authClient.validateToken(authHeader).getBody();
        log.info("Attempting to invite user '{}' to organization ID: {} by user ID: {}", requestDto.username(), organizationId, userId);

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
            log.warn("User ID: {} is not authorized to invite members to organization ID: {}", userId, organizationId);
            throw new NotAuthorizedToInviteException("User is not authorized to send invitations for this organization.");
        }
        log.debug("Inviter ID: {} is authorized (Owner: {}) for organization ID: {}", userId, isOwner,organizationId);


        UserDto invitedUserDto;
        try {
            log.debug("Calling Auth Service to find user by username: {}", requestDto.username());
            invitedUserDto = authServiceClient.findUserByUsername(requestDto.username());
            if (invitedUserDto == null || invitedUserDto.getId() == null) {
                throw new InvitedUserNotFoundException("Invited user not found: " + requestDto.username());
            }
            log.info("Found invited user '{}' with ID: {}", requestDto.username(), invitedUserDto.getId());
        } catch (FeignException.NotFound e) {
            log.error("Invited user not found via Auth Service. Username: {}", requestDto.username(), e);
            throw new InvitedUserNotFoundException("Invited user not found: " + requestDto.username());
        } catch (Exception e) {
            log.error("Error calling Auth Service for username: {}. Error: {}", requestDto.username(), e.getMessage(), e);
            throw new ServiceUnavailableException("Could not reach authentication service to verify user.");
        }

        Long invitedUserId = invitedUserDto.getId();

        if (userId.equals(invitedUserId)) {
            log.warn("User ID: {} attempted to invite themselves to organization ID: {}", userId, organizationId);
            throw new CannotInviteSelfException("You cannot invite yourself to the organization.");
        }

        if (userOrganizationRepository.existsByUserIdAndOrganizationId(invitedUserId, organizationId)) {
            log.warn("User ID: {} is already a member of organization ID: {}", invitedUserId, organizationId);
            throw new UserAlreadyMemberException("User '" + requestDto.username() + "' is already a member of this organization.");
        }
        if (inviteRepository.existsByOrganizationIdAndInvitedUserIdAndStatus(organizationId, invitedUserId, InviteStatus.PENDING)) {
            log.warn("A pending invitation already exists for user ID: {} in organization ID: {}", invitedUserId, organizationId);
            throw new InviteAlreadyPendingException("An invitation is already pending for user '" + requestDto.username() + "'.");
        }

        InviteEntity newInvite = InviteEntity.builder()
                .organization(organization)
                .invitedUserId(invitedUserId)
                .inviterUserId(userId)
                .status(InviteStatus.PENDING)
                .build();

        InviteEntity savedInvite = inviteRepository.save(newInvite);
        log.info("Successfully created invite ID: {} for user ID: {} to organization ID: {}", savedInvite.getId(), invitedUserId, organizationId);

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
        List<InviteEntity> pendingInvites = inviteRepository.findByInvitedUserIdAndStatusOrderByCreatedAtDesc(userId, InviteStatus.PENDING);

        return organizationMapper.entityToDtoList(pendingInvites);
    }





    @Transactional
    public InviteResponseDto acceptInvite(Long inviteId, String authHeader) throws AccessDeniedException {
        var userId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} attempting to accept invite ID: {}", userId, inviteId);

        InviteEntity invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> {
                    log.error("Invite not found with ID: {}", inviteId);
                    return new InviteNotFoundException("Invitation not found.");
                });

        if (!invite.getInvitedUserId().equals(userId)) {
            log.warn("Security violation: User ID {} tried to accept invite ID {} belonging to user ID {}",
                    userId, inviteId, invite.getInvitedUserId());
            throw new AccessDeniedException("You are not authorized to accept this invitation.");
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            log.warn("Invite ID: {} cannot be processed because its status is {}", inviteId, invite.getStatus());
            throw new InviteCannotBeProcessedException("This invitation cannot be processed (Status: " + invite.getStatus() + ").");
        }

        OrganizationEntity organization = invite.getOrganization();

        if (userOrganizationRepository.existsByUserIdAndOrganizationId(userId, organization.getId())) {
            log.warn("User ID: {} is already a member of organization ID: {} but tried to accept invite ID: {}. Setting invite to accepted.",
                    userId, organization.getId(), inviteId);
             throw new UserAlreadyMemberException("You are already a member of this organization.");
        } else {
            UserOrganizationEntity membership = UserOrganizationEntity.builder()
                    .userId(userId)
                    .organization(organization)
                    .role(UserRole.MEMBER)
                    .build();
            userOrganizationRepository.save(membership);
            log.info("User ID: {} successfully added to organization ID: {} with role: {}", userId, organization.getId(), UserRole.MEMBER);
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        InviteEntity updatedInvite = inviteRepository.save(invite);
        log.info("Invite ID: {} status updated to ACCEPTED", inviteId);

         try {
              notificationService.sendInviteAcceptedNotification(invite.getInviterUserId(), userId, organization.getName());
              log.info("Sent invite accepted notification to inviter ID: {}", invite.getInviterUserId());
         } catch (Exception e) {
              log.error("Failed to send invite accepted notification to inviter ID: {}. Error: {}", invite.getInviterUserId(), e.getMessage(), e);
         }

        return organizationMapper.mapToInviteResponseDto(updatedInvite);
    }

    @Transactional
    public InviteResponseDto rejectInvite(Long inviteId, String authHeader) throws AccessDeniedException {
        var userId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} attempting to reject invite ID: {}", userId, inviteId);

        InviteEntity invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> {
                    log.error("Invite not found with ID: {}", inviteId);
                    return new InviteNotFoundException("Invitation not found.");
                });

        if (!invite.getInvitedUserId().equals(userId)) {
            log.warn("Security violation: User ID {} tried to reject invite ID {} belonging to user ID {}",
                    userId, inviteId, invite.getInvitedUserId());
            throw new AccessDeniedException("You are not authorized to reject this invitation.");
        }

        if (invite.getStatus() != InviteStatus.PENDING) {
            log.warn("Invite ID: {} cannot be processed because its status is {}", inviteId, invite.getStatus());
            throw new InviteCannotBeProcessedException("This invitation cannot be processed (Status: " + invite.getStatus() + ").");
        }

        invite.setStatus(InviteStatus.REJECTED);
        InviteEntity updatedInvite = inviteRepository.save(invite);
        log.info("Invite ID: {} status updated to REJECTED", inviteId);

         try {
              notificationService.sendInviteRejectedNotification(invite.getInviterUserId(), userId, invite.getOrganization().getName());
              log.info("Sent invite rejected notification to inviter ID: {}", invite.getInviterUserId());
         } catch (Exception e) {
              log.error("Failed to send invite rejected notification to inviter ID: {}. Error: {}", invite.getInviterUserId(), e.getMessage(), e);
         }

        return organizationMapper.mapToInviteResponseDto(updatedInvite);
    }



}
