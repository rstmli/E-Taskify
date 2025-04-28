package az.etaskify.exception;

public class NotAuthorizedToInviteException extends RuntimeException {
    public NotAuthorizedToInviteException(String message) {
        super(message);
    }
}
