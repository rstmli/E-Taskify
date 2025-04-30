package az.etaskify.service;

import az.etaskify.client.AuthClient;
import az.etaskify.dao.entity.NotificationEntity;
import az.etaskify.dao.repository.NotificationRepository;
import az.etaskify.dto.UserDto;
import az.etaskify.util.enums.NotificationType;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthClient authServiceClient;

    public void sendInviteNotificationAsync(Long invitedUserId, Long inviterUserId, String organizationName) {
        log.info("Attempting to send invite notification. Invited User ID: {}, Inviter User ID: {}, Org Name: {}",
                invitedUserId, inviterUserId, organizationName);

        String inviterUsername = fetchUsername(inviterUserId);

        String message = String.format("'%s' sizi '%s' təşkilatına dəvət etdi.",
                inviterUsername,
                organizationName);

        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .userId(invitedUserId)
                    .message(message)
                    .isRead(false)
                    .type(NotificationType.INVITE_RECEIVED)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully saved invite notification for user ID: {}", invitedUserId);

        } catch (Exception e) {
            log.error("Failed to save invite notification for user ID: {}. Error: {}", invitedUserId, e.getMessage(), e);
        }
    }

    public void sendInviteAcceptedNotification(Long inviterUserId, Long acceptingUserId, String organizationName) {
        log.info("Attempting to send invite accepted notification. Inviter User ID: {}, Accepting User ID: {}, Org Name: {}",
                inviterUserId, acceptingUserId, organizationName);

        String acceptingUsername = fetchUsername(acceptingUserId);

        String message = String.format("'%s' sizin '%s' təşkilatına göndərdiyiniz dəvəti qəbul etdi.",
                acceptingUsername,
                organizationName);

        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .userId(inviterUserId)
                    .message(message)
                    .isRead(false)
                    .type(NotificationType.JOIN_ACCEPTED)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully saved invite accepted notification for user ID: {}", inviterUserId);

        } catch (Exception e) {
            log.error("Failed to save invite accepted notification for user ID: {}. Error: {}", inviterUserId, e.getMessage(), e);
        }
    }

    public void sendInviteRejectedNotification(Long inviterUserId, Long rejectingUserId, String organizationName) {
        log.info("Attempting to send invite rejected notification. Inviter User ID: {}, Rejecting User ID: {}, Org Name: {}",
                inviterUserId, rejectingUserId, organizationName);

        String rejectingUsername = fetchUsername(rejectingUserId);

        String message = String.format("'%s' sizin '%s' təşkilatına göndərdiyiniz dəvəti rədd etdi.",
                rejectingUsername,
                organizationName);

        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .userId(inviterUserId)
                    .message(message)
                    .isRead(false)
                    .type(NotificationType.INVITE_REJECTED)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully saved invite rejected notification for user ID: {}", inviterUserId);

        } catch (Exception e) {
            log.error("Failed to save invite rejected notification for user ID: {}. Error: {}", inviterUserId, e.getMessage(), e);
        }
    }

    public void sendJoinRequestReceivedNotification(Long ownerUserId, Long requestingUserId, String organizationName) {
        log.info("Attempting to send join request received notification. Owner User ID: {}, Requesting User ID: {}, Org Name: {}",
                ownerUserId, requestingUserId, organizationName);

        String requestingUsername = fetchUsername(requestingUserId);

        String message = String.format("'%s' adlı istifadəçi '%s' təşkilatınıza qoşulmaq üçün sorğu göndərdi.",
                requestingUsername,
                organizationName);

        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .userId(ownerUserId)
                    .message(message)
                    .isRead(false)
                    .type(NotificationType.JOIN_REQUEST_RECEIVED)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully saved join request received notification for owner user ID: {}", ownerUserId);

        } catch (Exception e) {
            log.error("Failed to save join request received notification for owner user ID: {}. Error: {}", ownerUserId, e.getMessage(), e);
        }
    }

    public void sendJoinRequestApprovedNotification(Long requestingUserId, String organizationName) {
        log.info("Attempting to send join request approved notification. Requesting User ID: {}, Org Name: {}",
                requestingUserId, organizationName);

        String message = String.format("'%s' təşkilatına qoşulma sorğunuz təsdiqləndi.",
                organizationName);

        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .userId(requestingUserId)
                    .message(message)
                    .isRead(false)
                    .type(NotificationType.JOIN_REQUEST_APPROVED)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully saved join request approved notification for user ID: {}", requestingUserId);

        } catch (Exception e) {
            log.error("Failed to save join request approved notification for user ID: {}. Error: {}", requestingUserId, e.getMessage(), e);
        }
    }

    public void sendJoinRequestRejectedNotification(Long requestingUserId, String organizationName) {
        log.info("Attempting to send join request rejected notification. Requesting User ID: {}, Org Name: {}",
                requestingUserId, organizationName);

        String message = String.format("'%s' təşkilatına qoşulma sorğunuz rədd edildi.",
                organizationName);

        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .userId(requestingUserId)
                    .message(message)
                    .isRead(false)
                    .type(NotificationType.JOIN_REQUEST_REJECTED)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully saved join request rejected notification for user ID: {}", requestingUserId);

        } catch (Exception e) {
            log.error("Failed to save join request rejected notification for user ID: {}. Error: {}", requestingUserId, e.getMessage(), e);
        }
    }


    private String fetchUsername(Long userId) {
        try {
            log.debug("Fetching username for user ID: {}", userId);
            UserDto userDto = authServiceClient.findUserById(userId);
            if (userDto != null && userDto.getUsername() != null) {
                log.debug("Username found: {}", userDto.getUsername());
                return userDto.getUsername();
            } else {
                log.warn("User DTO or username was null for user ID: {}", userId);
                return "Bir istifadəçi";
            }
        } catch (FeignException e) {
            log.error("Failed to fetch username for user ID: {} from Auth Service. Status: {}, Error: {}", userId, e.status(), e.getMessage());
            return "Bir istifadəçi";
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching username for user ID: {}. Error: {}", userId, e.getMessage(), e);
            return "Bir istifadəçi";
        }
    }




}