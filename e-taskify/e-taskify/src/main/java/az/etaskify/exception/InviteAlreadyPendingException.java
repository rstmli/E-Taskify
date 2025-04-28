package az.etaskify.exception;

public class InviteAlreadyPendingException extends RuntimeException {
    public InviteAlreadyPendingException(String message) {
        super(message);
    }
}
