package az.etaskify.dao.repository;

import az.etaskify.dao.entity.UserOrganizationEntity;
import feign.ResponseMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganizationEntity, Long> {
    ResponseMapper findByUserIdAndOrganizationId(Long inviterUserId, Long organizationId);

    boolean existsByUserIdAndOrganizationId(Long invitedUserId, Long organizationId);
}
