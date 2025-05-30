package az.etaskify.dto;

import az.etaskify.util.enums.VisibilityStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskResponseDto {
    private Long id;
    private Long organizationId;
    private String title;
    private String description;
    private Long createdByUserId;
    private String createdByUsername;
    private VisibilityStatus visibility;
    private LocalDateTime createdAt;
    private List<TaskAssigneeDto> assignees;
}