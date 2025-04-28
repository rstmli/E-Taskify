package az.etaskify.service;

import az.etaskify.client.AuthClient;
import az.etaskify.dao.entity.NotificationEntity;
import az.etaskify.dao.repository.NotificationRepository;
import az.etaskify.dto.UserDto;
import az.etaskify.util.enums.NotificationType;
import feign.FeignException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthClient authServiceClient;
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendInviteNotificationAsync(Long invitedUserId, Long inviterUserId, String organizationName) {
        log.info("Attempting to send invite notification. Invited User ID: {}, Inviter User ID: {}, Org Name: {}",
                invitedUserId, inviterUserId, organizationName);

        String inviterUsername = fetchUsername(inviterUserId);

        String message = String.format("'%s' sizi '%s' organizasyonuna davet etti.",
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void sendInviteAcceptedNotification(Long inviterUserId, Long acceptingUserId, String organizationName) {
        log.info("Attempting to send invite accepted notification. Inviter User ID: {}, Accepting User ID: {}, Org Name: {}",
                inviterUserId, acceptingUserId, organizationName);

        String acceptingUsername = fetchUsername(acceptingUserId);

        String message = String.format("'%s' sizin '%s' organizasyonuna gönderdiğiniz daveti kabul etti.",
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void sendInviteRejectedNotification(Long inviterUserId, Long rejectingUserId, String organizationName) {
        log.info("Attempting to send invite rejected notification. Inviter User ID: {}, Rejecting User ID: {}, Org Name: {}",
                inviterUserId, rejectingUserId, organizationName);

        String rejectingUsername = fetchUsername(rejectingUserId);

        String message = String.format("'%s' sizin '%s' organizasyonuna gönderdiğiniz daveti reddetti.",
                rejectingUsername,
                organizationName);

        try {
            NotificationEntity notification = NotificationEntity.builder()
                    .userId(inviterUserId)
                    .message(message)
                    .isRead(false)
                    .type(NotificationType.INVITE_RECEIVED)
                    .build();

            notificationRepository.save(notification);
            log.info("Successfully saved invite rejected notification for user ID: {}", inviterUserId);

        } catch (Exception e) {
            log.error("Failed to save invite rejected notification for user ID: {}. Error: {}", inviterUserId, e.getMessage(), e);
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
                return "Bir kullanıcı";
            }
        } catch (FeignException e) {
            log.error("Failed to fetch username for user ID: {} from Auth Service. Status: {}, Error: {}", userId, e.status(), e.getMessage());
            return "Bir kullanıcı";
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching username for user ID: {}. Error: {}", userId, e.getMessage(), e);
            return "Bir kullanıcı";
        }
    }


}