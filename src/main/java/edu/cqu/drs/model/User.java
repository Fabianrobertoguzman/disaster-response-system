package edu.cqu.drs.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DRS user account with a {@link UserRole}.
 *
 * <p>Realises the data side of Assessment&nbsp;One's FR-12 ("allow Administrators to create, suspend,
 * revoke and reassign user accounts and role memberships"). The prototype models the account itself;
 * the administrator <em>UI</em> for managing accounts is a Stage-3 item. {@code User} instances are
 * also referenced by {@link AuditLog} entries for recovery-phase compliance review.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class User implements Serializable {

    /** Serialisation version (users travel over the client/server protocol). */
    private static final long serialVersionUID = 1L;

    /** Immutable unique identifier. */
    private final UUID id;

    /** Login name (never null or blank). */
    private String username;

    /** Assigned role (never null). */
    private UserRole role;

    /**
     * Creates a user account.
     *
     * @param username the login name (must not be null or blank).
     * @param role     the assigned role (must not be null).
     * @throws IllegalArgumentException if {@code username} is null or blank, or {@code role} is null.
     */
    public User(String username, UserRole role) {
        requireNonBlankUsername(username);
        requireNonNullRole(role);
        this.id = UUID.randomUUID();
        this.username = username;
        this.role = role;
    }

    /**
     * Reconstruction constructor used only by the persistence tier to rebuild a
     * user from a stored database row, preserving the persisted identity rather
     * than generating a new one.
     *
     * @param id       the persisted identifier (must not be null).
     * @param username the login name (must not be null or blank).
     * @param role     the assigned role (must not be null).
     * @throws IllegalArgumentException if {@code id} is null, {@code username} is
     *         null or blank, or {@code role} is null.
     */
    public User(UUID id, String username, UserRole role) {
        requireNonBlankUsername(username);
        requireNonNullRole(role);
        if (id == null) {
            throw new IllegalArgumentException("id is required to reconstruct a user");
        }
        this.id = id;
        this.username = username;
        this.role = role;
    }

    /** @throws IllegalArgumentException if {@code username} is null or blank. */
    private static void requireNonBlankUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be null or blank");
        }
    }

    /** @throws IllegalArgumentException if {@code role} is null. */
    private static void requireNonNullRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
    }

    /** @return the immutable user identifier. */
    public UUID getId() {
        return this.id;
    }

    /** @return the login name. */
    public String getUsername() {
        return this.username;
    }

    /** @param username the new login name (must not be null or blank). @throws IllegalArgumentException if invalid. */
    public void setUsername(String username) {
        requireNonBlankUsername(username);
        this.username = username;
    }

    /** @return the assigned role. */
    public UserRole getRole() {
        return this.role;
    }

    /** @param role the new role (must not be null). @throws IllegalArgumentException if null. */
    public void setRole(UserRole role) {
        requireNonNullRole(role);
        this.role = role;
    }

    /** @return true if this user holds the {@link UserRole#ADMINISTRATOR} role. */
    public boolean isAdministrator() {
        return this.role == UserRole.ADMINISTRATOR;
    }

    /**
     * Hand-written display string for audit-log readability (Object's default toString returns Class@hash).
     *
     * @return e.g. {@code "User{id=..., username=meiru, role=DISPATCHER}"}.
     */
    @Override
    public String toString() {
        return "User{id=" + this.id + ", username=" + this.username + ", role=" + this.role + "}";
    }

    /**
     * Identity equality on the immutable {@link #id}.
     *
     * @param other the object to compare with.
     * @return true if {@code other} is a {@code User} with the same id.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        return this.id.equals(((User) other).id);
    }

    /** @return a hash code consistent with {@link #equals(Object)} (based on {@link #id}). */
    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
