package az.etaskify.exception;

public class JoinRequestCannotBeProcessedException extends RuntimeException {
    public JoinRequestCannotBeProcessedException(String message) {
        super(message);
    }
}
