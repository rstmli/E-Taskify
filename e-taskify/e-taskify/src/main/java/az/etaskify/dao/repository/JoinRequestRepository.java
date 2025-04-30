package az.etaskify.dao.repository;

import az.etaskify.dao.entity.JoinRequestEntity;
import az.etaskify.util.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JoinRequestRepository extends JpaRepository<JoinRequestEntity,Long> {
    boolean existsByOrganizationIdAndUserIdAndStatus(Long organizationId, Long userId, InviteStatus status);
    List<JoinRequestEntity> findByOrganizationIdAndStatus(Long organizationId, InviteStatus status);
    Optional<JoinRequestEntity> findByIdAndStatus(Long id, InviteStatus status);
}
