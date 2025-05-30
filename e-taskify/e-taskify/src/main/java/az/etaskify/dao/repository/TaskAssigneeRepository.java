package az.etaskify.dao.repository;

import az.etaskify.dao.entity.TaskAssigneeEntity;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // Eğer deleteBy... kullanıyorsak

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssigneeRepository extends JpaRepository<TaskAssigneeEntity, Long> {

     @Query("SELECT ta.userId FROM TaskAssigneeEntity ta WHERE ta.taskEntity.id = :taskId")
     List<Long> findUserIdsByTaskId(@Param("taskId") Long taskId);

    List<TaskAssigneeEntity> findByTaskEntityId(Long taskId);

    boolean existsByTaskEntityIdAndUserId(Long taskId, Long userId);

    List<TaskAssigneeEntity> findByUserId(Long userId);

    @Transactional
    void deleteByTaskEntityIdAndUserId(Long taskId, Long userId);

    @Transactional
    void deleteByTaskEntityId(Long taskId);
}