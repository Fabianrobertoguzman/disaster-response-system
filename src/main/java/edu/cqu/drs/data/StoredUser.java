package edu.cqu.drs.data;

import edu.cqu.drs.model.User;

/**
 * A user account together with its stored credential material (the password hash
 * and the salt), as read from the {@code users} table.
 *
 * <p>The domain {@link User} deliberately carries no password, so this data-tier
 * record is the only place the hash and salt travel. It is returned by
 * {@link UserDao#findByUsername(String)} so the authentication service can verify
 * a supplied password against the stored hash without the credential material
 * leaking into the domain model or across the client/server protocol.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public final class StoredUser {

    /** The user account (no credential material). */
    private final User user;

    /** The stored password hash (Base64). */
    private final String passwordHash;

    /** The stored per-user salt (Base64). */
    private final String salt;

    /**
     * Creates a stored-user record.
     *
     * @param user         the user account (must not be null).
     * @param passwordHash the stored password hash (must not be null).
     * @param salt         the stored salt (must not be null).
     * @throws IllegalArgumentException if any argument is null.
     */
    public StoredUser(User user, String passwordHash, String salt) {
        if (user == null || passwordHash == null || salt == null) {
            throw new IllegalArgumentException(
                    "user, passwordHash and salt are required");
        }
        this.user = user;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    /** @return the user account. */
    public User getUser() {
        return this.user;
    }

    /** @return the stored password hash. */
    public String getPasswordHash() {
        return this.passwordHash;
    }

    /** @return the stored salt. */
    public String getSalt() {
        return this.salt;
    }
}
