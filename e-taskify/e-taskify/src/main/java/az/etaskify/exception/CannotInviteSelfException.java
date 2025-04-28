package az.etaskify.exception;

public class CannotInviteSelfException extends RuntimeException {
    public CannotInviteSelfException(String message) {
        super(message);
    }
}
