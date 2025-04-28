package az.etaskify.auth.dto;

public record JwtUserInfo(Long userId, String email, String username, String role) {
}
