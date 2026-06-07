package edu.cqu.drs.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Genuine reversible field-level encryption with AES-256 in GCM mode - the
 * Assessment-3 §2.5 encryption/decryption measure.
 *
 * <p>Unlike password hashing (which is one-way), this provides true
 * confidentiality with authenticity: {@link #decrypt(String) decrypt}({@link
 * #encrypt(String) encrypt}(x)) == x, and any tampering with the ciphertext is
 * detected by the GCM authentication tag. A fresh random 12-byte IV is generated
 * for every encryption (so the same plaintext encrypts to different ciphertext
 * each time) and is prepended to the ciphertext; the whole thing is Base64
 * encoded for storage in a text column.</p>
 *
 * <p>The key is supplied at construction (loaded from configuration in
 * production, or generated for tests/first run). It is the field the system uses
 * to protect a sensitive stored value, satisfying §2.5 with a reversible cipher
 * rather than mislabelling password hashing as encryption.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public class FieldCipher {

    /** AES in Galois/Counter Mode, no padding (GCM is a stream mode). */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** Recommended GCM IV length in bytes. */
    private static final int IV_BYTES = 12;

    /** GCM authentication tag length in bits. */
    private static final int TAG_BITS = 128;

    /** AES key length in bits. */
    public static final int KEY_BITS = 256;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates a cipher over an existing key.
     *
     * @param key the AES key (must not be null).
     * @throws IllegalArgumentException if {@code key} is null.
     */
    public FieldCipher(SecretKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        this.key = key;
    }

    /**
     * Builds a cipher from a Base64-encoded AES key (the production path - the key
     * is loaded from configuration).
     *
     * @param base64Key the Base64 AES key.
     * @return a cipher over that key.
     */
    public static FieldCipher fromBase64Key(String base64Key) {
        if (base64Key == null) {
            throw new IllegalArgumentException("base64Key must not be null");
        }
        return new FieldCipher(new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES"));
    }

    /**
     * Builds a cipher over a freshly generated key (tests / first run when no key
     * is configured).
     *
     * @return a cipher over a new random AES-256 key.
     */
    public static FieldCipher withGeneratedKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(KEY_BITS);
            return new FieldCipher(generator.generateKey());
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Could not generate an AES key", ex);
        }
    }

    /**
     * @return the key as Base64, so a generated key can be persisted to config.
     */
    public String exportKeyBase64() {
        return Base64.getEncoder().encodeToString(this.key.getEncoded());
    }

    /**
     * Encrypts plaintext, returning IV-prepended ciphertext as Base64.
     *
     * @param plaintext the value to encrypt; null returns null.
     * @return the Base64 ciphertext (a fresh IV each call), or null.
     * @throws SecurityException if encryption fails.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            this.random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, this.key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Encryption failed", ex);
        }
    }

    /**
     * Decrypts Base64 IV-prepended ciphertext produced by {@link #encrypt(String)}.
     *
     * @param ciphertextBase64 the Base64 ciphertext; null returns null.
     * @return the recovered plaintext, or null.
     * @throws SecurityException if decryption or authentication fails (e.g. the
     *         data was tampered with or the wrong key was used).
     */
    public String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertextBase64);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_BYTES, combined.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, this.key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Decryption failed (tampered data or wrong key)", ex);
        }
    }
}
