package az.etaskify.dto;

import az.etaskify.util.enums.VisibilityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TaskCreateRequestDto {
    @NotBlank(message = "Title cannot be blank")
    private String title;
    private String description;
    @NotNull(message = "Visibility cannot be null")
    private VisibilityStatus visibility;
    private List<Long> assigneeUserIds;
}