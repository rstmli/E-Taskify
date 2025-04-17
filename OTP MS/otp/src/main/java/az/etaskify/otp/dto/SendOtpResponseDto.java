package az.etaskify.otp.dto;

import az.etaskify.otp.util.enums.OtpStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@AllArgsConstructor
@Getter
@Setter
@Builder
public class SendOtpResponseDto {
    private OtpStatus otpStatus;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String blockTime;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String expireTime;

}
