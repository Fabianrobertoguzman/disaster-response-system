package edu.cqu.drs.data;

import edu.cqu.drs.model.User;
import edu.cqu.drs.model.UserRole;
import edu.cqu.drs.security.PasswordHasher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for {@link UserDaoImpl} against the selected JDBC backend
 * (MySQL, or in-memory H2 under {@code -Ptest-h2}) - the persistence half of
 * the access-rights security measure: a user's PBKDF2 hash and salt must
 * round-trip the {@code users} table intact and verify against the original
 * password, while the credential material never leaks through the plain reads.
 * Skipped (not failed) when no database is reachable.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("UserDao - user accounts and credential material (MySQL/H2)")
class UserDaoSpec {

    private UserDao dao;
    private PasswordHasher hasher;

    @BeforeAll
    static void requireDatabase() {
        assumeTrue(DatabaseTestSupport.available(),
                "Database not reachable - UserDao integration tests skipped");
    }

    @BeforeEach
    void setUp() throws Exception {
        this.dao = new UserDaoImpl(DatabaseTestSupport.freshDatabase());
        this.hasher = new PasswordHasher();
    }

    @Test
    @DisplayName("the stored hash and salt round-trip and verify against the original password")
    void shouldRoundTripCredentialMaterial() {
        String salt = this.hasher.newSalt();
        String hash = this.hasher.hash("s3cret-pw", salt);
        User user = new User("meiru", UserRole.DISPATCHER);

        this.dao.insert(user, hash, salt);
        StoredUser stored = this.dao.findByUsername("meiru").orElseThrow();

        assertEquals(hash, stored.getPasswordHash());
        assertEquals(salt, stored.getSalt());
        assertTrue(this.hasher.matches("s3cret-pw", stored.getSalt(), stored.getPasswordHash()),
                "the persisted credential must verify against the original password");
        assertEquals(user.getId(), stored.getUser().getId());
        assertEquals(UserRole.DISPATCHER, stored.getUser().getRole());
    }

    @Test
    @DisplayName("the plain reads expose the account but never the credential material")
    void shouldExposeAccountWithoutCredentials() {
        User user = new User("auditor1", UserRole.AUDITOR);
        this.dao.insert(user, this.hasher.hash("pw", this.hasher.newSalt()),
                this.hasher.newSalt());

        User byUuid = this.dao.findByUuid(user.getId()).orElseThrow();
        assertEquals("auditor1", byUuid.getUsername());
        assertEquals(UserRole.AUDITOR, byUuid.getRole());
        assertEquals(1, this.dao.findAll().size());
    }

    @Test
    @DisplayName("a duplicate username is rejected by the unique constraint")
    void shouldRejectDuplicateUsername() {
        String salt = this.hasher.newSalt();
        this.dao.insert(new User("meiru", UserRole.DISPATCHER),
                this.hasher.hash("pw", salt), salt);

        assertThrows(DataAccessException.class,
                () -> this.dao.insert(new User("meiru", UserRole.CITIZEN),
                        this.hasher.hash("other", salt), salt));

        // The rejection must leave the original account untouched - exactly one
        // row, still holding the first registration's role.
        assertEquals(1, this.dao.findAll().size());
        assertEquals(UserRole.DISPATCHER,
                this.dao.findByUsername("meiru").orElseThrow().getUser().getRole());
    }

    @Test
    @DisplayName("lookups for unknown users return empty")
    void shouldReturnEmptyForUnknownUsers() {
        assertTrue(this.dao.findByUsername("nobody").isEmpty());
        assertTrue(this.dao.findByUuid(UUID.randomUUID()).isEmpty());
        assertTrue(this.dao.findAll().isEmpty());
    }
}
