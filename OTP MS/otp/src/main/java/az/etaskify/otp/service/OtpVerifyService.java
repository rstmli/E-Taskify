package az.etaskify.otp.service;

import az.etaskify.otp.dao.entity.OtpEntity;
import az.etaskify.otp.dao.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpVerifyService {
    private final OtpRepository repository;

    public ResponseEntity<String> emailVerify(String email, String otp) {
        var emailOtp = repository.findByEmail(email);

        if (emailOtp.isEmpty()) {
            return ResponseEntity.badRequest().body("Email hesabı tapılmadı.");
        }

        OtpEntity emailData = emailOtp.get();

        if (emailData.getExpireTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("OTP kodunun müddəti bitib.");
        }

        if (!emailData.getOtpCode().equals(otp)) {
            return ResponseEntity.badRequest().body("OTP kodu səhvdir.");
        }
        repository.delete(emailData);
        return ResponseEntity.ok("Email uğurla doğrulandı.");
    }

}
