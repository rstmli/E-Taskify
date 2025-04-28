package az.etaskify.exception;

public class CannotInviteToPublicOrganizationException extends RuntimeException {
    public CannotInviteToPublicOrganizationException(String message) {
        super(message);
    }
}
