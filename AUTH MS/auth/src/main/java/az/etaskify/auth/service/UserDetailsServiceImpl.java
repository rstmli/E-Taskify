package az.etaskify.auth.service;

import az.etaskify.auth.client.OtpClient;
import az.etaskify.auth.dao.entity.UsersEntity;
import az.etaskify.auth.dao.repository.UsersRepository;
import az.etaskify.auth.dto.LoginResponse;
import az.etaskify.auth.dto.SendOtpResponseDto;
import az.etaskify.auth.dto.UserLoginRequestDto;
import az.etaskify.auth.dto.UserRegisterRequestDto;
import az.etaskify.auth.exception.InvalidUsernameException;
import az.etaskify.auth.util.enums.OtpStatus;
import az.etaskify.auth.util.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UsersRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;
    @Lazy
    @Autowired
    private AuthenticationManager authenticationManager;
    private final JwtHelper jwtHelper;
    private final OtpClient otpClient;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = repository.findByUsername(username).orElseThrow(() ->
                new InvalidUsernameException(username));
        return new User(username, user.getPassword(), List.of());
    }

    public String register(UserRegisterRequestDto dto){
        if(!dto.getPassword().equals(dto.getCurrentPassword())){
            throw new RuntimeException();
        }
        if(repository.findByUsername(dto.getUsername()).isPresent()){
            throw new RuntimeException();
        }
        if(repository.findByEmail(dto.getEmail()).isPresent()){
            throw new RuntimeException("Email already exists");
        }

        var hashedPassword = passwordEncoder.encode(dto.getPassword());

        SendOtpResponseDto otpResponse = otpClient.sendOtp(dto.getEmail());
        if (!otpResponse.getOtpStatus().equals(OtpStatus.PENDING)) {
            throw new RuntimeException("Failed to send OTP.");
        }

        var newUser = UsersEntity.builder()
                        .email(dto.getEmail())
                        .username(dto.getUsername())
                        .password(hashedPassword)
                .role(dto.getRole())
                        .isActive(false)
                        .build();

        var savedUser = repository.save(newUser);

        return savedUser.getUsername();


    }

    public String verifyOtpAndActivateUser(String username, String otpCode) {
        UsersEntity user = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        ResponseEntity<String> verifyResponse = otpClient.verifyOtp(user.getEmail(), otpCode);

        if (!verifyResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("OTP doğrulama başarısız: " + verifyResponse.getBody());
        }

        user.setIsActive(true);
        repository.save(user);

        return "Email doğrulama başarılı. Hesabınız aktif edildi.";
    }


    public LoginResponse login(UserLoginRequestDto dto){
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getUsername(),
                dto.getPassword()));

        UsersEntity user = repository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new InvalidUsernameException("user not found!"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Email verification not done.");
        }

        String token = jwtHelper.tokenGenerate(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name()
        );
        return new LoginResponse(token, null);
    }

}