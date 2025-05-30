package az.etaskify.exception;

public class UserNotMemberOfOrganizationException extends RuntimeException {
    public UserNotMemberOfOrganizationException(String message) {
        super(message);
    }
}
