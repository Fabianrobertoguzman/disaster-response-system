package edu.cqu.drs.security;

/**
 * Thrown when authentication or authorisation fails: a bad username/password, a
 * missing or expired session token, or a role that is not permitted for the
 * requested action. The server's dispatcher maps it to a protocol
 * {@code UNAUTHORIZED} response.
 *
 * <p>The message is deliberately non-specific for bad credentials (it does not
 * reveal whether the username or the password was wrong) to avoid user
 * enumeration.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an authentication/authorisation exception.
     *
     * @param message a non-sensitive description of the failure.
     */
    public AuthException(String message) {
        super(message);
    }
}
