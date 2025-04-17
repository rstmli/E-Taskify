package az.etaskify.otp.service;

import az.etaskify.otp.dao.entity.OtpEntity;
import az.etaskify.otp.dao.repository.OtpRepository;
import az.etaskify.otp.dto.SendOtpResponseDto;
import az.etaskify.otp.util.enums.OtpStatus;
import az.etaskify.otp.util.helper.EmailSender;
import az.etaskify.otp.util.helper.OtpCodeGenerater;
import az.etaskify.otp.util.helper.OtpDateFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpSendService {

    private final OtpCodeGenerater otpCodeGenerater;
    private final EmailSender emailSender;
    private final OtpRepository repository;

    public SendOtpResponseDto sendOtp(String email) {

        var otpEntity = repository.findByEmail(email);
        if(otpEntity.isPresent()){
            var otpData = otpEntity.get();
            if(otpData.getStatus().equals(OtpStatus.BLOCK)){
                if(otpData.getBlockTime().isAfter(LocalDateTime.now())){
                    return sendOtpResponseDto(otpData);
                }else{
                    removeData(email);
                    var entity = sendOtpFirstTime(email);
                    emailSender.sendOtpEmail(email,entity.getOtpCode());
                    return sendOtpResponseDto(entity);

                }
            }else{
                if(otpData.getEmailCount() >= 5){
                    otpData.setStatus(OtpStatus.BLOCK);
                    otpData.setBlockTime(LocalDateTime.now().plusMinutes(5));
                    otpData.setExpireTime(null);
                    var entity = repository.save(otpData);
                    return sendOtpResponseDto(entity);
                }else{
                    otpData.setEmailCount(otpData.getEmailCount() + 1);
                    otpData.setStatus(OtpStatus.PENDING);
                    otpData.setOtpCode(otpCodeGenerater.codeGenerator());
                    var entity = repository.save(otpData);
                    emailSender.sendOtpEmail(email,entity.getOtpCode());
                    return sendOtpResponseDto(entity);
                }
            }
        }else {
            var entity = sendOtpFirstTime(email);
            emailSender.sendOtpEmail(email,entity.getOtpCode());
            return sendOtpResponseDto(entity);
        }
    }

    private OtpEntity sendOtpFirstTime(String email) {
        var code = otpCodeGenerater.codeGenerator();
        var expireTime = LocalDateTime.now().plusMinutes(5);
        var entity = OtpEntity.builder().otpCode(code)
                .email(email)
                .status(OtpStatus.PENDING)
                .emailCount(1)
                .expireTime(expireTime)
                .build();
        return repository.save(entity);
    }

    private SendOtpResponseDto sendOtpResponseDto(OtpEntity entity) {
        return new SendOtpResponseDto(
                entity.getStatus(),
                OtpDateFormatter.dateFormatter(entity.getBlockTime()),
                OtpDateFormatter.dateFormatter(entity.getExpireTime())
        );
    }
    private void removeData (String email){
        var entity = repository.findByEmail(email);
        repository.delete(entity.get());
    }
}
