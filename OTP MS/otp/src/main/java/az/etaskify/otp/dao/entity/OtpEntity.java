package az.etaskify.otp.dao.entity;

import az.etaskify.otp.util.enums.OtpStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "otp")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Data
public class OtpEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    @Column(name = "emailCount")
    private Integer emailCount;
    @Column(name = "verifyCount")
    private Integer verifyCount;
    private String otpCode;
    @Enumerated(EnumType.STRING)
    private OtpStatus status;
    private LocalDateTime expireTime;
    private LocalDateTime blockTime;
}
