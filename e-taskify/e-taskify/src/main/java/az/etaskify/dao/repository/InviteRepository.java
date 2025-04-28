package az.etaskify.dao.repository;

import az.etaskify.dao.entity.InviteEntity;
import az.etaskify.util.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<InviteEntity,Long> {
    boolean existsByOrganizationIdAndInvitedUserIdAndStatus(Long organizationId, Long organizationId1, InviteStatus status);
    List<InviteEntity> findByInvitedUserIdAndStatusOrderByCreatedAtDesc(Long invitedUserId, InviteStatus status);
    Optional<InviteEntity> findByIdAndInvitedUserId(Long id, Long invitedUserId);
}
