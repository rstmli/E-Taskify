package az.etaskify.dao.repository;

import az.etaskify.dao.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findByOrganizationId(Long organizationId);

    List<TaskEntity> findByOrganizationIdAndCreatedBy(Long organizationId, Long createdBy);

    Optional<TaskEntity> findByIdAndOrganizationId(Long id, Long organizationId);
}