package az.etaskify.otp.service;

import az.etaskify.otp.dao.entity.OtpEntity;
import az.etaskify.otp.dao.repository.OtpRepository;
import az.etaskify.otp.util.enums.OtpStatus;
import az.etaskify.otp.util.helper.EmailSender;
import az.etaskify.otp.util.helper.OtpCodeGenerater;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpSendService {

    private final OtpCodeGenerater otpCodeGenerater;
    private final EmailSender emailSender;
    private final OtpRepository repository;

    public ResponseEntity<String> sendOtp(String email) {
        var otp = otpCodeGenerater.codeGenerator();

        var otpEntity = new OtpEntity().builder()
                .email(email)
                .otpCode(otp)
                .status(OtpStatus.PENDING)
                .expireTime(LocalDateTime.now().plusMinutes(5))
                .build();
        repository.save(otpEntity);
        emailSender.sendOtpEmail(email,otp);
        return ResponseEntity.ok("otp gonderildi :)");
    }
}
