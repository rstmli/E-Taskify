package az.etaskify.exception;

public class JoinRequestNotFoundException extends RuntimeException {
    public JoinRequestNotFoundException(String message) {
        super(message);
    }
}
