package az.etaskify.auth.comtroller;

import az.etaskify.auth.dto.LoginResponse;
import az.etaskify.auth.dto.UserLoginRequestDto;
import az.etaskify.auth.dto.UserRegisterRequestDto;
import az.etaskify.auth.service.UserDetailsServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth/")
public class AuthController {
    private final UserDetailsServiceImpl service;


    @PostMapping("/register")
    public String registerUser(@RequestBody @Valid UserRegisterRequestDto dto) {
        return service.register(dto);
    }

    @PostMapping("/email-verify")
    public String emailVerify(@RequestParam String username,@RequestParam String otpCode){
        return service.verifyOtpAndActivateUser(username,otpCode);
    }
    @PostMapping("/login")
    public LoginResponse userLogin (@RequestBody UserLoginRequestDto dto){
        return service.login(dto);
    }
}
