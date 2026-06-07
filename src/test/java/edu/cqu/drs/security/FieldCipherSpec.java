package edu.cqu.drs.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link FieldCipher} (AES-256-GCM reversible encryption - the §2.5
 * encryption/decryption measure). Pure and deterministic; no database or network.
 *
 * @author Fabian Roberto Guzman (12287570)
 */
@DisplayName("FieldCipher - AES-GCM reversible encryption")
class FieldCipherSpec {

    @Test
    @DisplayName("decrypt(encrypt(x)) recovers the original plaintext")
    void shouldRoundTrip() {
        FieldCipher cipher = FieldCipher.withGeneratedKey();
        String plaintext = "Casualty at 12 Pine St, apartment 4B";
        assertEquals(plaintext, cipher.decrypt(cipher.encrypt(plaintext)));
    }

    @Test
    @DisplayName("the same plaintext encrypts to different ciphertext each time (random IV)")
    void shouldUseFreshIv() {
        FieldCipher cipher = FieldCipher.withGeneratedKey();
        assertNotEquals(cipher.encrypt("repeat"), cipher.encrypt("repeat"));
    }

    @Test
    @DisplayName("tampered ciphertext is rejected by the authentication tag")
    void shouldDetectTampering() {
        FieldCipher cipher = FieldCipher.withGeneratedKey();
        String encrypted = cipher.encrypt("integrity matters");
        String tampered = flipLastChar(encrypted);
        assertThrows(SecurityException.class, () -> cipher.decrypt(tampered));
    }

    @Test
    @DisplayName("a different key cannot decrypt the ciphertext")
    void shouldFailWithWrongKey() {
        FieldCipher writer = FieldCipher.withGeneratedKey();
        FieldCipher other = FieldCipher.withGeneratedKey();
        String encrypted = writer.encrypt("for my eyes only");
        assertThrows(SecurityException.class, () -> other.decrypt(encrypted));
    }

    @Test
    @DisplayName("an exported key reconstructs a cipher that decrypts the same ciphertext")
    void shouldRoundTripExportedKey() {
        FieldCipher writer = FieldCipher.withGeneratedKey();
        String encrypted = writer.encrypt("portable secret");
        FieldCipher reloaded = FieldCipher.fromBase64Key(writer.exportKeyBase64());
        assertEquals("portable secret", reloaded.decrypt(encrypted));
    }

    @Test
    @DisplayName("null encrypts and decrypts to null")
    void shouldHandleNull() {
        FieldCipher cipher = FieldCipher.withGeneratedKey();
        assertNull(cipher.encrypt(null));
        assertNull(cipher.decrypt(null));
    }

    /**
     * Flips the last character of a string to simulate tampering.
     *
     * @param value the string to alter.
     * @return the altered string.
     */
    private static String flipLastChar(String value) {
        char last = value.charAt(value.length() - 1);
        char replacement = (last == 'A') ? 'B' : 'A';
        return value.substring(0, value.length() - 1) + replacement;
    }
}
