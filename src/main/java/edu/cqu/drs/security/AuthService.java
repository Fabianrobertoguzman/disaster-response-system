package edu.cqu.drs.security;

import edu.cqu.drs.data.AuditDao;
import edu.cqu.drs.data.AuditEntry;
import edu.cqu.drs.data.StoredUser;
import edu.cqu.drs.data.UserDao;
import edu.cqu.drs.model.User;
import edu.cqu.drs.model.UserRole;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The access-rights and session authority for DRS-Enhanced - the server-side
 * heart of the §2.5 security measures.
 *
 * <p>It registers users with a salted PBKDF2 hash (never a plaintext password),
 * authenticates a username/password against the stored hash and, on success,
 * issues an opaque random session token bound to the user. {@link
 * #requireRole(String, UserRole...)} is the per-request authorisation gate the
 * server's dispatcher calls before each protected action. Every register, login
 * and logout is written to the audit trail (non-repudiation, FR-14).</p>
 *
 * <p>Thread-safe: the session map is a {@link ConcurrentHashMap} and the DAOs are
 * themselves safe for concurrent use, so one instance is shared by all server
 * worker threads.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class AuthService {

    /** Session-token length in bytes. */
    private static final int TOKEN_BYTES = 32;

    private final UserDao userDao;
    private final PasswordHasher hasher;
    private final AuditDao auditDao;

    /** Throwaway salt + hash, used to spend equal work on an unknown username so a
     *  failed login takes the same time whether or not the user exists
     *  (closing the user-enumeration timing side-channel). */
    private final String dummySalt;
    private final String dummyHash;

    /** Active sessions: token -> authenticated user. */
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    /** Strong source for session tokens. */
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates the authentication service.
     *
     * @param userDao  the user store (must not be null).
     * @param hasher   the password hasher (must not be null).
     * @param auditDao the audit trail (must not be null).
     * @throws IllegalArgumentException if any dependency is null.
     */
    public AuthService(UserDao userDao, PasswordHasher hasher, AuditDao auditDao) {
        if (userDao == null || hasher == null || auditDao == null) {
            throw new IllegalArgumentException("all dependencies are required");
        }
        this.userDao = userDao;
        this.hasher = hasher;
        this.auditDao = auditDao;
        this.dummySalt = hasher.newSalt();
        this.dummyHash = hasher.hash("not-a-real-password", this.dummySalt);
    }

    /**
     * Registers a new user, storing a salted PBKDF2 hash of the password.
     *
     * @param username the login name (must not be null or blank).
     * @param password the plaintext password (must not be null).
     * @param role     the assigned role (must not be null).
     * @return the created user (no credential material).
     * @throws edu.cqu.drs.data.DataAccessException if the username already exists.
     */
    public User register(String username, String password, UserRole role) {
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        String salt = this.hasher.newSalt();
        String hash = this.hasher.hash(password, salt);
        User user = new User(username, role);
        this.userDao.insert(user, hash, salt);
        this.auditDao.record(new AuditEntry(
                user.getId(), null, "REGISTER_USER role=" + role, "User", user.getId()));
        return user;
    }

    /**
     * Authenticates a user and opens a session.
     *
     * @param username the login name.
     * @param password the plaintext password.
     * @return a fresh session token.
     * @throws AuthException if the username is unknown or the password is wrong.
     */
    public String login(String username, String password) {
        StoredUser stored = (username == null) ? null
                : this.userDao.findByUsername(username).orElse(null);
        // Always run the PBKDF2 comparison - against the real credential if the
        // user exists, otherwise against a throwaway one - so the response time
        // does not reveal whether the username exists (anti-enumeration).
        String salt = (stored != null) ? stored.getSalt() : this.dummySalt;
        String expectedHash = (stored != null) ? stored.getPasswordHash() : this.dummyHash;
        boolean credentialsValid = this.hasher.matches(password, salt, expectedHash);
        if (stored == null || !credentialsValid) {
            throw new AuthException("invalid username or password");
        }
        String token = newToken();
        this.sessions.put(token, stored.getUser());
        this.auditDao.record(new AuditEntry(
                stored.getUser().getId(), null, "LOGIN", "User", stored.getUser().getId()));
        return token;
    }

    /**
     * Resolves a token to its user.
     *
     * @param token the session token (may be null).
     * @return the user if the token is a live session, otherwise empty.
     */
    public Optional<User> resolve(String token) {
        return (token == null) ? Optional.empty() : Optional.ofNullable(this.sessions.get(token));
    }

    /**
     * Authorisation gate: the token must resolve to a user holding one of the
     * allowed roles.
     *
     * @param token   the session token.
     * @param allowed the roles permitted for the action.
     * @return the authenticated, authorised user.
     * @throws AuthException if the token is invalid or the role is not permitted.
     */
    public User requireRole(String token, UserRole... allowed) {
        User user = resolve(token).orElseThrow(
                () -> new AuthException("authentication required"));
        for (UserRole role : allowed) {
            if (user.getRole() == role) {
                return user;
            }
        }
        throw new AuthException(
                "role " + user.getRole() + " is not permitted for this action");
    }

    /**
     * Ends a session (idempotent).
     *
     * @param token the session token (may be null/unknown).
     */
    public void logout(String token) {
        if (token == null) {
            return;
        }
        User user = this.sessions.remove(token);
        if (user != null) {
            this.auditDao.record(new AuditEntry(
                    user.getId(), null, "LOGOUT", "User", user.getId()));
        }
    }

    /** @return the number of live sessions (diagnostic / test aid). */
    public int activeSessionCount() {
        return this.sessions.size();
    }

    /**
     * @return a fresh URL-safe random session token.
     */
    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        this.random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
