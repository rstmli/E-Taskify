package az.etaskify.otp.controller;
import az.etaskify.otp.service.OtpSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpSendService otpService;

    @PostMapping("/send")
    public ResponseEntity<String> sendOtp(@RequestParam String email) {
        return otpService.sendOtp(email);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        return null;
    }
}
