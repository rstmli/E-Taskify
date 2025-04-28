package az.etaskify.auth.comtroller;

import az.etaskify.auth.dto.UserDto;
import az.etaskify.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/user")
@CrossOrigin(origins = "http://localhost:8083")
public class UserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public UserDto userById(@PathVariable("id") Long id){
        return userService.getByUser(id);
    }

    @GetMapping("/search/{username}")
    public UserDto userByUsername(@PathVariable("username") String username) {
        return userService.getByUsername(username);
    }
}
