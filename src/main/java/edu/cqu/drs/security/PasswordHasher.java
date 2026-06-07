package edu.cqu.drs.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * One-way password <strong>hashing</strong> with PBKDF2-HMAC-SHA-256.
 *
 * <p>This is the access-rights half of the Assessment-3 §2.5 security measures:
 * passwords are never stored or transmitted in the clear, only as a salted,
 * iterated hash. It is deliberately <em>not</em> the §2.5 encryption/decryption
 * measure - hashing is one-way and cannot be reversed; the genuine reversible
 * measure is {@link FieldCipher} (AES-GCM).</p>
 *
 * <p>Each password gets a fresh 16-byte random salt; the hash uses
 * {@value #ITERATIONS} iterations and a {@value #KEY_LENGTH_BITS}-bit derived
 * key. Verification recomputes the hash and compares it in constant time to
 * resist timing attacks.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class PasswordHasher {

    /** PBKDF2 with HMAC-SHA-256. */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /** Iteration count (work factor). */
    public static final int ITERATIONS = 210_000;

    /** Derived-key length in bits. */
    public static final int KEY_LENGTH_BITS = 256;

    /** Salt length in bytes. */
    private static final int SALT_BYTES = 16;

    /** Cryptographically strong source of salt bytes. */
    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a fresh random salt.
     *
     * @return a Base64-encoded 16-byte salt.
     */
    public String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        this.random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password with the given salt.
     *
     * @param password   the plaintext password (must not be null).
     * @param saltBase64 the Base64 salt from {@link #newSalt()} (must not be null).
     * @return the Base64-encoded PBKDF2 hash.
     * @throws IllegalArgumentException if an argument is null.
     * @throws SecurityException if the platform lacks the algorithm.
     */
    public String hash(String password, String saltBase64) {
        if (password == null || saltBase64 == null) {
            throw new IllegalArgumentException("password and salt are required");
        }
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new SecurityException("Password hashing failed", ex);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Verifies a password against a stored salt and hash, in constant time.
     *
     * @param password           the plaintext password to check.
     * @param saltBase64         the stored salt.
     * @param expectedHashBase64 the stored hash.
     * @return true if the password matches.
     */
    public boolean matches(String password, String saltBase64, String expectedHashBase64) {
        if (password == null || saltBase64 == null || expectedHashBase64 == null) {
            return false;
        }
        byte[] actual = Base64.getDecoder().decode(hash(password, saltBase64));
        byte[] expected;
        try {
            expected = Base64.getDecoder().decode(expectedHashBase64);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return MessageDigest.isEqual(actual, expected);
    }
}
