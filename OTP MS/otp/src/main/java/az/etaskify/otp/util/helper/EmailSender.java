package az.etaskify.otp.util.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSender {
    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("OTP Kodunuz");
        message.setText("OTP kodunuz: " + otp);
        mailSender.send(message);
    }
}
