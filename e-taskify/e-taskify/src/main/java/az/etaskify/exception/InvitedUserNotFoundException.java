package az.etaskify.exception;

public class InvitedUserNotFoundException extends RuntimeException {
    public InvitedUserNotFoundException(String message) {
        super(message);
    }
}
