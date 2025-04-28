package az.etaskify.auth.comtroller;

import az.etaskify.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/token")
public class TokenController {
    private final JwtService jwtService;
    @CrossOrigin(origins = "http://localhost:8083")
    @GetMapping("/validate")
    public ResponseEntity<Long> validateToken(@RequestHeader("Authorization") String authHeader){
        return jwtService.validateToken(authHeader);
    }
}
