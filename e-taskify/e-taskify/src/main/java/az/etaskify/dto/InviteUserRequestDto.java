package az.etaskify.dto;

import jakarta.validation.constraints.NotBlank;

public record InviteUserRequestDto ( @NotBlank(message = "Username of the user to invite cannot be blank")
                                     String username) {


}