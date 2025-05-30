package az.etaskify.service;

import az.etaskify.client.AuthClient;
import az.etaskify.dao.entity.OrganizationEntity;
import az.etaskify.dao.entity.TaskAssigneeEntity;
import az.etaskify.dao.entity.TaskEntity;
import az.etaskify.dao.repository.*;
import az.etaskify.dto.*;
import az.etaskify.exception.*;
import az.etaskify.util.enums.VisibilityStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final AuthClient authClient;

    @Transactional
    public TaskResponseDto createTask(Long organizationId, TaskCreateRequestDto dto, String authHeader) {
        Long currentUserId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} attempting to create task in Organization ID: {}", currentUserId, organizationId);

        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found with ID: " + organizationId));

        if (!userOrganizationRepository.existsByUserIdAndOrganizationId(currentUserId, organizationId)) {
            log.warn("User ID: {} is not a member of Organization ID: {}", currentUserId, organizationId);
            throw new UserNotMemberOfOrganizationException("User is not a member of this organization.");
        }

        TaskEntity taskEntity = TaskEntity.builder()
                .organization(organization)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .createdBy(currentUserId)
                .visibility(dto.getVisibility())
                .build();

        TaskEntity savedTask = taskRepository.save(taskEntity);
        log.info("Task created with ID: {} by User ID: {}", savedTask.getId(), currentUserId);

        List<TaskAssigneeEntity> assignees = new ArrayList<>();
        if (dto.getVisibility() == VisibilityStatus.CUSTOM) {
            if (dto.getAssigneeUserIds() == null || dto.getAssigneeUserIds().isEmpty()) {
                log.warn("Custom visibility task created without assignees. Task ID: {}", savedTask.getId());
                 throw new InvalidTaskAssignmentException("Custom visibility tasks must have at least one assignee.");
            } else {
                Set<Long> uniqueAssigneeIds = new HashSet<>(dto.getAssigneeUserIds());

                for (Long assigneeId : uniqueAssigneeIds) {
                     if (assigneeId.equals(currentUserId)) continue;

                    if (!userOrganizationRepository.existsByUserIdAndOrganizationId(assigneeId, organizationId)) {
                        log.warn("Attempted to assign non-member User ID: {} to Task ID: {}", assigneeId, savedTask.getId());
                        throw new InvalidTaskAssignmentException("User with ID " + assigneeId + " is not a member of this organization.");
                    }

                    TaskAssigneeEntity assignee = TaskAssigneeEntity.builder()
                            .taskEntity(savedTask)
                            .userId(assigneeId)
                            .build();
                    assignees.add(assignee);
                }
                if (!assignees.isEmpty()) {
                    taskAssigneeRepository.saveAll(assignees);
                    log.info("Assigned {} users to custom Task ID: {}", assignees.size(), savedTask.getId());
                }
            }
        }

        return mapToTaskResponseDto(savedTask, assignees);
    }


    public TaskResponseDto getTaskById(Long taskId, String authHeader) throws AccessDeniedException {
        Long currentUserId = authClient.validateToken(authHeader).getBody();
        log.debug("User ID: {} attempting to access Task ID: {}", currentUserId, taskId);

        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));

        Long organizationId = task.getOrganization().getId();

        if (!userOrganizationRepository.existsByUserIdAndOrganizationId(currentUserId, organizationId)) {
            log.warn("Access denied: User ID: {} is not a member of Organization ID: {} for Task ID: {}", currentUserId, organizationId, taskId);
            throw new AccessDeniedException("You are not a member of the organization this task belongs to.");
        }

        if (task.getVisibility() == VisibilityStatus.PUBLIC) {
            log.debug("Public task access granted for Task ID: {}", taskId);
            List<TaskAssigneeEntity> assignees = (task.getVisibility() == VisibilityStatus.CUSTOM)
                                                 ? taskAssigneeRepository.findByTaskEntityId(taskId)
                                                 : Collections.emptyList();
            return mapToTaskResponseDto(task, assignees);
        } else {
            boolean isCreator = task.getCreatedBy().equals(currentUserId);
            boolean isAssignee = taskAssigneeRepository.existsByTaskEntityIdAndUserId(taskId, currentUserId);

            if (isCreator || isAssignee) {
                log.debug("Custom task access granted for Task ID: {}. IsCreator: {}, IsAssignee: {}", taskId, isCreator, isAssignee);
                List<TaskAssigneeEntity> assignees = taskAssigneeRepository.findByTaskEntityId(taskId);
                return mapToTaskResponseDto(task, assignees);
            } else {
                log.warn("Access denied: User ID: {} is not creator or assignee for Custom Task ID: {}", currentUserId, taskId);
                throw new AccessDeniedException("You are not authorized to view this task.");
            }
        }
    }


    public List<TaskResponseDto> listVisibleTasksForOrganization(Long organizationId, String authHeader) {
        Long currentUserId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} listing tasks for Organization ID: {}", currentUserId, organizationId);

        if (!userOrganizationRepository.existsByUserIdAndOrganizationId(currentUserId, organizationId)) {
            log.warn("User ID: {} is not a member of Organization ID: {}", currentUserId, organizationId);
            return Collections.emptyList();
        }

        List<TaskEntity> allTasks = taskRepository.findByOrganizationId(organizationId);

        List<TaskResponseDto> visibleTasks = allTasks.stream()
                .filter(task -> {
                    if (task.getVisibility() == VisibilityStatus.PUBLIC) {
                        return true;
                    } else {
                        return task.getCreatedBy().equals(currentUserId) ||
                               taskAssigneeRepository.existsByTaskEntityIdAndUserId(task.getId(), currentUserId);
                    }
                })
                .map(task -> {
                     List<TaskAssigneeEntity> assignees = (task.getVisibility() == VisibilityStatus.CUSTOM)
                                                 ? taskAssigneeRepository.findByTaskEntityId(task.getId())
                                                 : Collections.emptyList();
                    return mapToTaskResponseDto(task, assignees);
                })
                .collect(Collectors.toList());

        log.info("Found {} visible tasks for User ID: {} in Organization ID: {}", visibleTasks.size(), currentUserId, organizationId);
        return visibleTasks;
    }

    @Transactional
    public TaskResponseDto updateTask(Long taskId, Long organizationId, TaskUpdateRequestDto dto, String authHeader) throws AccessDeniedException {
        Long currentUserId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} attempting to update Task ID: {} in Organization ID: {}", currentUserId, taskId, organizationId);

        TaskEntity task = taskRepository.findByIdAndOrganizationId(taskId, organizationId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId + " in organization ID: " + organizationId));

        if (!task.getCreatedBy().equals(currentUserId)) {
            log.warn("User ID: {} is not authorized to update Task ID: {} (not creator)", currentUserId, taskId);
            throw new AccessDeniedException("You are not authorized to update this task.");
        }

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setVisibility(dto.getVisibility());

        taskAssigneeRepository.deleteByTaskEntityId(taskId);
        log.debug("Cleared existing assignees for Task ID: {}", taskId);

        List<TaskAssigneeEntity> newAssignees = new ArrayList<>();
        if (dto.getVisibility() == VisibilityStatus.CUSTOM) {
            if (dto.getAssigneeUserIds() == null || dto.getAssigneeUserIds().isEmpty()) {
                log.warn("Custom visibility task updated without assignees. Task ID: {}", taskId);
                 throw new InvalidTaskAssignmentException("Custom visibility tasks must have at least one assignee when updated.");
            } else {
                Set<Long> uniqueAssigneeIds = dto.getAssigneeUserIds().stream().collect(Collectors.toSet());
                for (Long assigneeId : uniqueAssigneeIds) {
                    if (!userOrganizationRepository.existsByUserIdAndOrganizationId(assigneeId, organizationId)) {
                        log.warn("Attempted to assign non-member User ID: {} to Task ID: {} during update", assigneeId, taskId);
                        throw new InvalidTaskAssignmentException("User with ID " + assigneeId + " is not a member of this organization.");
                    }
                    TaskAssigneeEntity assignee = TaskAssigneeEntity.builder()
                            .taskEntity(task)
                            .userId(assigneeId)
                            .build();
                    newAssignees.add(assignee);
                }
                if (!newAssignees.isEmpty()) {
                    taskAssigneeRepository.saveAll(newAssignees);
                    log.info("Updated assignees for custom Task ID: {}. Count: {}", taskId, newAssignees.size());
                }
            }
        }

        TaskEntity updatedTask = taskRepository.save(task);
        log.info("Task ID: {} updated successfully by User ID: {}", taskId, currentUserId);

        return mapToTaskResponseDto(updatedTask, newAssignees);
    }


    @Transactional
    public void deleteTask(Long taskId, Long organizationId, String authHeader) throws AccessDeniedException {
        Long currentUserId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} attempting to delete Task ID: {} from Organization ID: {}", currentUserId, taskId, organizationId);

        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));

        if (!task.getOrganization().getId().equals(organizationId)) {
            log.error("Mismatch: Task ID {} (Org ID: {}) does not belong to provided Organization ID: {}",
                    taskId, task.getOrganization().getId(), organizationId);
            throw new TaskNotFoundException("Task not found within the specified organization.");
        }

        boolean isOwner = task.getOrganization().getOwnerId().equals(currentUserId);
        boolean isCreator = task.getCreatedBy().equals(currentUserId);

        if (!isOwner && !isCreator) {
            log.warn("User ID: {} is not authorized to delete Task ID: {} (not owner or creator)", currentUserId, taskId);
            throw new AccessDeniedException("You are not authorized to delete this task.");
        }

        taskAssigneeRepository.deleteByTaskEntityId(taskId);
        log.debug("Deleted all assignees for Task ID: {}", taskId);

        taskRepository.delete(task);
        log.info("Task ID: {} deleted successfully by User ID: {} (IsOwner: {}, IsCreator: {})", taskId, currentUserId, isOwner, isCreator);
    }


    public List<TaskResponseDto> listMyAssignedTasks(Long organizationId, String authHeader) {
        Long currentUserId = authClient.validateToken(authHeader).getBody();
        log.info("User ID: {} listing tasks assigned to them in Organization ID: {}", currentUserId, organizationId);

        if (!userOrganizationRepository.existsByUserIdAndOrganizationId(currentUserId, organizationId)) {
            log.warn("User ID: {} is not a member of Organization ID: {}, cannot list assigned tasks.", currentUserId, organizationId);
            return Collections.emptyList();
        }

        List<TaskAssigneeEntity> myAssignments = taskAssigneeRepository.findByUserId(currentUserId);

        List<TaskResponseDto> assignedTasksInOrg = myAssignments.stream()
                .map(TaskAssigneeEntity::getTaskEntity)
                .filter(Objects::nonNull)
                .filter(task -> task.getOrganization() != null && task.getOrganization().getId().equals(organizationId))
                .map(task -> {
                    List<TaskAssigneeEntity> assigneesForThisTask = taskAssigneeRepository.findByTaskEntityId(task.getId());
                    return mapToTaskResponseDto(task, assigneesForThisTask);
                })
                .collect(Collectors.toList());

        log.info("Found {} tasks assigned to User ID: {} in Organization ID: {}", assignedTasksInOrg.size(), currentUserId, organizationId);
        return assignedTasksInOrg;
    }


    private TaskResponseDto mapToTaskResponseDto(TaskEntity task, List<TaskAssigneeEntity> assignees) {
        if (task == null) return null;

        String creatorUsername = "Unknown";
        try {
            UserDto creatorDto = authClient.findUserById(task.getCreatedBy());
            if (creatorDto != null && creatorDto.getUsername() != null) {
                creatorUsername = creatorDto.getUsername();
            }
        } catch (Exception e) {
            log.error("Could not fetch username for creator ID: {}", task.getCreatedBy(), e);
        }

        List<TaskAssigneeDto> assigneeDtos = new ArrayList<>();
        if (assignees != null && !assignees.isEmpty()) {
            for (TaskAssigneeEntity assignee : assignees) {
                String assigneeUsername = "Unknown";
                try {
                    UserDto assigneeDto = authClient.findUserById(assignee.getUserId());
                    if (assigneeDto != null && assigneeDto.getUsername() != null) {
                        assigneeUsername = assigneeDto.getUsername();
                    }
                } catch (Exception e) {
                    log.error("Could not fetch username for assignee ID: {}", assignee.getUserId(), e);
                }
                assigneeDtos.add(TaskAssigneeDto.builder()
                        .userId(assignee.getUserId())
                        .username(assigneeUsername)
                        .build());
            }
        }

        return TaskResponseDto.builder()
                .id(task.getId())
                .organizationId(task.getOrganization().getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .createdByUserId(task.getCreatedBy())
                .createdByUsername(creatorUsername)
                .visibility(task.getVisibility())
                .createdAt(task.getCreatedAt())
                .assignees(assigneeDtos)
                .build();
    }
}