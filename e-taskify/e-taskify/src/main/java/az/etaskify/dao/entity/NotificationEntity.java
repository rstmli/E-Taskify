package az.etaskify.dao.entity;

import az.etaskify.util.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_id",nullable = false)
    Long userId;
    String message;
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    Boolean isRead;
    @Enumerated(EnumType.STRING)
    NotificationType type;
    @CreationTimestamp
    LocalDateTime createdAt;
}
