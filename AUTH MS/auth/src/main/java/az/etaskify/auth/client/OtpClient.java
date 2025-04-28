package az.etaskify.auth.client;

import az.etaskify.auth.dto.SendOtpResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "otp-service", url = "${otp.url}")
public interface OtpClient {
    @PostMapping("/send")
    SendOtpResponseDto sendOtp(@RequestParam String email);

    @PostMapping("/verify")
    ResponseEntity<String> verifyOtp(@RequestParam String email, @RequestParam String otp);
}
