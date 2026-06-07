package edu.cqu.drs.security;

import edu.cqu.drs.data.DataAccessException;
import edu.cqu.drs.data.InMemoryDataStore;
import edu.cqu.drs.model.User;
import edu.cqu.drs.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AuthService} over the in-memory store - registration,
 * login, role authorisation, logout and audit. Pure and deterministic.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("AuthService - access rights and sessions")
class AuthServiceSpec {

    private InMemoryDataStore store;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        this.store = new InMemoryDataStore();
        this.auth = new AuthService(this.store.userDao(), new PasswordHasher(), this.store.auditDao());
    }

    @Test
    @DisplayName("a registered user can log in and the token resolves to them")
    void shouldRegisterAndLogin() {
        this.auth.register("meiru", "s3cret-pw", UserRole.DISPATCHER);
        String token = this.auth.login("meiru", "s3cret-pw");
        assertNotNull(token);
        Optional<User> resolved = this.auth.resolve(token);
        assertTrue(resolved.isPresent());
        assertEquals("meiru", resolved.get().getUsername());
    }

    @Test
    @DisplayName("the password is stored hashed, never in clear text")
    void shouldStoreHashedPassword() {
        this.auth.register("meiru", "s3cret-pw", UserRole.DISPATCHER);
        String storedHash = this.store.userDao().findByUsername("meiru").orElseThrow()
                .getPasswordHash();
        assertNotEquals("s3cret-pw", storedHash);
    }

    @Test
    @DisplayName("a wrong password is rejected")
    void shouldRejectWrongPassword() {
        this.auth.register("meiru", "s3cret-pw", UserRole.DISPATCHER);
        assertThrows(AuthException.class, () -> this.auth.login("meiru", "guess"));
    }

    @Test
    @DisplayName("an unknown user is rejected")
    void shouldRejectUnknownUser() {
        assertThrows(AuthException.class, () -> this.auth.login("nobody", "x"));
    }

    @Test
    @DisplayName("requireRole admits a permitted role and rejects others")
    void shouldEnforceRoles() {
        this.auth.register("meiru", "pw", UserRole.DISPATCHER);
        String token = this.auth.login("meiru", "pw");
        assertEquals("meiru",
                this.auth.requireRole(token, UserRole.DISPATCHER).getUsername());
        assertThrows(AuthException.class,
                () -> this.auth.requireRole(token, UserRole.ADMINISTRATOR));
    }

    @Test
    @DisplayName("requireRole rejects a missing or invalid token")
    void shouldRejectInvalidToken() {
        assertThrows(AuthException.class,
                () -> this.auth.requireRole(null, UserRole.DISPATCHER));
        assertThrows(AuthException.class,
                () -> this.auth.requireRole("not-a-token", UserRole.DISPATCHER));
    }

    @Test
    @DisplayName("logout invalidates the session token")
    void shouldLogout() {
        this.auth.register("meiru", "pw", UserRole.DISPATCHER);
        String token = this.auth.login("meiru", "pw");
        this.auth.logout(token);
        assertTrue(this.auth.resolve(token).isEmpty());
    }

    @Test
    @DisplayName("registering a duplicate username is rejected")
    void shouldRejectDuplicateUsername() {
        this.auth.register("meiru", "pw", UserRole.DISPATCHER);
        assertThrows(DataAccessException.class,
                () -> this.auth.register("meiru", "other", UserRole.CITIZEN));
    }

    @Test
    @DisplayName("login is recorded in the audit trail")
    void shouldAuditLogin() {
        this.auth.register("meiru", "pw", UserRole.DISPATCHER);
        this.auth.login("meiru", "pw");
        assertTrue(this.store.auditDao().findAll().stream()
                .anyMatch(entry -> "LOGIN".equals(entry.getAction())));
    }

    @Test
    @DisplayName("audit entries carry a server-assigned timestamp (the §2.5 time-stamping measure)")
    void shouldTimestampAuditEntries() {
        this.auth.register("meiru", "pw", UserRole.DISPATCHER);
        this.auth.login("meiru", "pw");
        assertFalse(this.store.auditDao().findAll().isEmpty());
        assertTrue(this.store.auditDao().findAll().stream()
                .allMatch(entry -> entry.getTimestamp() != null));
    }
}
