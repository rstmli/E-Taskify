package az.etaskify.otp.util.helper;

import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
public class OtpCodeGenerater {

    public String codeGenerator() {
        return String.format("%06d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 1_000_000L);
    }
}
