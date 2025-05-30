package az.etaskify.controller;

import az.etaskify.dto.TaskCreateRequestDto;
import az.etaskify.dto.TaskResponseDto;
import az.etaskify.dto.TaskUpdateRequestDto;
import az.etaskify.service.TaskService;
import jakarta.validation.Valid; // Jakarta EE 9+ için
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/v1/organizations/{organizationId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(
            @PathVariable Long organizationId,
            @Valid @RequestBody TaskCreateRequestDto taskCreateRequestDto,
            @RequestHeader("Authorization") String authHeader) {
        TaskResponseDto createdTask = taskService.createTask(organizationId, taskCreateRequestDto, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }


    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> getTaskById(
            @PathVariable Long organizationId,
            @PathVariable Long taskId,
            @RequestHeader("Authorization")String authHeader) throws AccessDeniedException {
        log.info("API Request: Get task by ID: {} in Organization ID: {}", taskId, organizationId);
        TaskResponseDto task = taskService.getTaskById(taskId, authHeader);
        return ResponseEntity.ok(task);
    }


    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> listVisibleTasksForOrganization(
            @PathVariable Long organizationId,
            @RequestHeader("Authorization") String authHeader) {
        List<TaskResponseDto> tasks = taskService.listVisibleTasksForOrganization(organizationId, authHeader);
        return ResponseEntity.ok(tasks);
    }


    @GetMapping("/assigned-to-me")
    public ResponseEntity<List<TaskResponseDto>> listMyAssignedTasks(
            @PathVariable Long organizationId,
            @RequestHeader("Authorization") String authHeader) {
        List<TaskResponseDto> tasks = taskService.listMyAssignedTasks(organizationId, authHeader);
        return ResponseEntity.ok(tasks);
    }


    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> updateTask(
            @PathVariable Long organizationId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequestDto taskUpdateRequestDto,
            @RequestHeader("Authorization") String authHeader) throws AccessDeniedException {
        TaskResponseDto updatedTask = taskService.updateTask(taskId, organizationId, taskUpdateRequestDto, authHeader);
        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long organizationId,
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String authHeader) throws AccessDeniedException {
        taskService.deleteTask(taskId, organizationId, authHeader);
        return ResponseEntity.noContent().build();
    }
}