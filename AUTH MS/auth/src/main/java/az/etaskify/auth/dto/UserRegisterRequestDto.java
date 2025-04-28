package az.etaskify.auth.dto;

import az.etaskify.auth.util.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRegisterRequestDto {
    @NotBlank
    @Email
    String email;
    @NotBlank
    @Size(min = 8,max = 16, message = "symbol length 8-16")
    String username;
    @NotBlank
    @Size(min= 6, max = 32, message = "password length min 6 max 32")
    String password;
    Role role;
    @NotBlank
    String currentPassword;
}
