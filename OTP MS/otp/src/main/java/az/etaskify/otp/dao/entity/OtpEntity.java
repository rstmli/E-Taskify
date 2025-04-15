package az.etaskify.otp.dao.entity;

import az.etaskify.otp.util.enums.OtpStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "otp")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class OtpEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String otpCode;
    @Enumerated(EnumType.STRING)
    private OtpStatus status;
    private LocalDateTime expireTime;
    private LocalDateTime blockTime;
}
