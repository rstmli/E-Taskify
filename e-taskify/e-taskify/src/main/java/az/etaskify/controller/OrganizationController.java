package az.etaskify.controller;

import az.etaskify.dto.*;
import az.etaskify.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.ServiceUnavailableException;
import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("api/v1/organization")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;

    @PostMapping("/create")
    public ResponseEntity<String> createOrg(@RequestBody OrganizationCreateRequest dto, @RequestHeader("Authorization") String authHeader) {
        return organizationService.createOrganization(dto, authHeader);
    }
    @PostMapping("/invite-org/{id}")
    public InviteResponseDto inviteUserToOrganization(@PathVariable("id") Long organizationId,@RequestBody InviteUserRequestDto requestDto,@RequestHeader("Authorization") String authHeader) throws ServiceUnavailableException {
        return organizationService.inviteUserToOrganization(organizationId,requestDto,authHeader);
    }
    @GetMapping("show-invite")
    public List<InviteResponseDto> showAllPendingInvite(@RequestHeader("Authorization") String authHeader){
        return organizationService.listMyPendingInvites(authHeader);
    }

    @PostMapping("accept-invite/{id}")
    public InviteResponseDto acceptInvite(@PathVariable("id") Long inviteId,@RequestHeader("Authorization") String authHeader) throws AccessDeniedException {
        return organizationService.acceptInvite(inviteId,authHeader);
    }

    @PostMapping("rejected-invite/{id}")
    public InviteResponseDto rejectInvite(@PathVariable("id") Long inviteId,@RequestHeader("Authorization") String authHeader) throws AccessDeniedException {
        return organizationService.rejectInvite(inviteId,authHeader);
    }

    @GetMapping("/search")
    public List<PublicOrganizationDto> findPublicOrganizations(@RequestParam("team") String search){
        return organizationService.findPublicOrganizations(search);
    }
    @PostMapping("/invite-org-public/{id}")
    public JoinRequestDto requestToJoinOrganization(@PathVariable("id") Long organizationId,@RequestHeader("Authorization") String authHeader) {
        return organizationService.requestToJoinOrganization(organizationId,authHeader);
    }
    @GetMapping("/invite-join/{id}")
    public List<JoinRequestDto> listPendingJoinRequests(@PathVariable("id") Long organizationId,@RequestHeader("Authorization") String authHeader) {
        return organizationService.listPendingJoinRequests(organizationId,authHeader);
    }

    @PostMapping("join-accept-invite/{id}")
    public JoinRequestDto approveJoinRequest(@PathVariable("id") Long requestId,@RequestHeader("Authorization") String authHeader){
        return organizationService.approveJoinRequest(requestId,authHeader);
    }

    @PostMapping("join-rejected-invite/{id}")
    public JoinRequestDto rejectJoinRequest(@PathVariable("id") Long requestId,@RequestHeader("Authorization") String authHeader){
        return organizationService.rejectJoinRequest(requestId,authHeader);
    }
}
