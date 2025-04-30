package az.etaskify.exception;

public class JoinRequestAlreadyPendingException extends RuntimeException {
    public JoinRequestAlreadyPendingException(String message) {
        super(message);
    }
}
