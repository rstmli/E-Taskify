package az.etaskify.dto;

import az.etaskify.util.enums.InviteStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JoinRequestDto {
    private Long id;
    private Long userId;
    private Long organizationId;
    private String organizationName;
    private InviteStatus status;
    private LocalDateTime createdAt;
    private String requestingUsername;
}
