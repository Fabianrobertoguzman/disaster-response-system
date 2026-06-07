package edu.cqu.drs.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PasswordHasher} (PBKDF2 password hashing). Pure and
 * deterministic; no database or network.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("PasswordHasher - salted PBKDF2 hashing")
class PasswordHasherSpec {

    private PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        this.hasher = new PasswordHasher();
    }

    @Test
    @DisplayName("the correct password verifies against its stored salt and hash")
    void shouldVerifyCorrectPassword() {
        String salt = this.hasher.newSalt();
        String hash = this.hasher.hash("correct horse", salt);
        assertTrue(this.hasher.matches("correct horse", salt, hash));
    }

    @Test
    @DisplayName("a wrong password does not verify")
    void shouldRejectWrongPassword() {
        String salt = this.hasher.newSalt();
        String hash = this.hasher.hash("correct horse", salt);
        assertFalse(this.hasher.matches("wrong horse", salt, hash));
    }

    @Test
    @DisplayName("the same password under different salts produces different hashes")
    void shouldSaltHashes() {
        String saltA = this.hasher.newSalt();
        String saltB = this.hasher.newSalt();
        assertNotEquals(saltA, saltB);
        assertNotEquals(this.hasher.hash("pw", saltA), this.hasher.hash("pw", saltB));
    }

    @Test
    @DisplayName("the same password and salt hash deterministically")
    void shouldBeDeterministicForSameSalt() {
        String salt = this.hasher.newSalt();
        assertTrue(this.hasher.hash("pw", salt).equals(this.hasher.hash("pw", salt)));
    }

    @Test
    @DisplayName("verifying against the wrong salt fails")
    void shouldRejectWrongSalt() {
        String salt = this.hasher.newSalt();
        String hash = this.hasher.hash("pw", salt);
        assertFalse(this.hasher.matches("pw", this.hasher.newSalt(), hash));
    }
}
