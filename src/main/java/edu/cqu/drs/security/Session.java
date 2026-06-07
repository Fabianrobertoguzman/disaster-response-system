package edu.cqu.drs.security;

import edu.cqu.drs.model.User;

import java.io.Serializable;

/**
 * The result of a successful login, returned to the client: the opaque session
 * token to send on subsequent requests, and the authenticated {@link User} (so
 * the client can adapt its UI to the user's role).
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class Session implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The opaque session token. */
    private final String token;

    /** The authenticated user (no credential material). */
    private final User user;

    /**
     * Creates a session.
     *
     * @param token the session token (must not be null).
     * @param user  the authenticated user (must not be null).
     * @throws IllegalArgumentException if either argument is null.
     */
    public Session(String token, User user) {
        if (token == null || user == null) {
            throw new IllegalArgumentException("token and user are required");
        }
        this.token = token;
        this.user = user;
    }

    /** @return the session token. */
    public String getToken() {
        return this.token;
    }

    /** @return the authenticated user. */
    public User getUser() {
        return this.user;
    }

    /**
     * Hand-written display string (the token is redacted so it never reaches a log).
     *
     * @return e.g. {@code Session{user=User{...}, token=present}}.
     */
    @Override
    public String toString() {
        return "Session{user=" + this.user + ", token=present}";
    }
}
