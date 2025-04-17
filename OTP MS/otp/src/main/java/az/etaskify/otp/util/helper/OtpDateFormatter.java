package az.etaskify.otp.util.helper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OtpDateFormatter {
    public static String dateFormatter(LocalDateTime time){
        if(time == null) {
            return null;
        }
        return time.format(DateTimeFormatter.ofPattern("dd:MM:yyyy H:m:s"));
    }
}
