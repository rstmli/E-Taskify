package az.etaskify.auth.dto;

import az.etaskify.auth.util.enums.OtpStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendOtpResponseDto {
    private OtpStatus otpStatus;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String blockTime;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String expireTime;
}
