package az.etaskify.dao.entity;

import az.etaskify.util.enums.InviteStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "invite")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InviteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "inviter_user_id", nullable = false)
    Long inviterUserId;
    @Column(name = "invited_user_id", nullable = false)
    Long invitedUserId;
    @Enumerated(EnumType.STRING)
    InviteStatus status;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    OrganizationEntity organization;
    @CreationTimestamp
    LocalDateTime createdAt;
}
