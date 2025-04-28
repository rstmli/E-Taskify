package az.etaskify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteUserRequestDto {

    @NotBlank(message = "Username of the user to invite cannot be blank")
    private String username;

}