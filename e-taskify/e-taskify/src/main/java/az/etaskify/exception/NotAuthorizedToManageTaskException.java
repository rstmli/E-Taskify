package az.etaskify.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotAuthorizedToManageTaskException extends RuntimeException {
    public NotAuthorizedToManageTaskException(String message) {
        super(message);
    }
}