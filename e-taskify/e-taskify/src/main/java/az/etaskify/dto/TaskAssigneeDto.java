package az.etaskify.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskAssigneeDto {
    private Long userId;
    private String username;
}