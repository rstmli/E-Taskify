package az.etaskify.auth.util.jwt;

import az.etaskify.auth.dto.JwtUserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@RequiredArgsConstructor
@Component
public class JwtHelper {
    @Value("${jwt.expire}")
    private Long EXPIRE;
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    public String tokenGenerate(Long userid,String email, String username,String role){
        return Jwts.builder()
                .subject(userid.toString())
                .claim("email", email)
                .claim("username", username)
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(secretKey())
                .compact();

    }


    public JwtUserInfo tokenByDecoder(String token) {
        var claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new JwtUserInfo(
                Long.parseLong(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("username", String.class),
                claims.get("role", String.class)
        );
    }
    private SecretKey secretKey (){
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String subject = claims.getSubject();
        if (subject == null) {
            throw new RuntimeException("Subject (User ID) claim is missing or null in the token");
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Subject (User ID) claim is not a valid number: " + subject, e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
