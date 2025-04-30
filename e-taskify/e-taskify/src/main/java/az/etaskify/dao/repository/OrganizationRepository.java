package az.etaskify.dao.repository;

import az.etaskify.dao.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {
    List<OrganizationEntity> findByNameContainingIgnoreCaseAndIsPrivateFalse(String name);
    List<OrganizationEntity> findByIsPrivateFalse();
}
