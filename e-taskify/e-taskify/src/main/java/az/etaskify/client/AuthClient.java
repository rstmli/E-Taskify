package az.etaskify.client;

import az.etaskify.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service",url = "${auth.url}")
public interface AuthClient {
    @GetMapping("user/{id}")
    UserDto findUserById(@PathVariable("id") Long id);

    @GetMapping("/user/search/{username}")
    UserDto findUserByUsername(@PathVariable("username") String username);

    @GetMapping("token/validate")
    ResponseEntity<Long> validateToken(@RequestHeader("Authorization") String token);

}
