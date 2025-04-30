package az.etaskify.exception;

public class CannotRequestToJoinPrivateOrganizationException extends RuntimeException {
    public CannotRequestToJoinPrivateOrganizationException(String message) {
        super(message);
    }
}
