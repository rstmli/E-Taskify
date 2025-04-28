package az.etaskify.dto;


import az.etaskify.util.enums.InviteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteResponseDto {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private Long invitedUserId;
    private Long inviterUserId;
    private InviteStatus status;
    private LocalDateTime createdAt;

}