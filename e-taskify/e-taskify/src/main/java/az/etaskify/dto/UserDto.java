package az.etaskify.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    Long id;
    String username;
    String email;
}
