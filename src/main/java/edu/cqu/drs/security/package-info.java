/**
 * Security tier of DRS-Enhanced, delivering the Assessment-3 §2.5 measures.
 *
 * <ul>
 *   <li><b>Access rights</b> - {@link edu.cqu.drs.security.AuthService} registers
 *       users, authenticates them and gates each protected action by role;
 *       {@link edu.cqu.drs.security.PasswordHasher} stores passwords as a salted,
 *       iterated PBKDF2 <em>hash</em> (one-way, never reversible).</li>
 *   <li><b>Encryption / decryption</b> - {@link edu.cqu.drs.security.FieldCipher}
 *       provides genuine <em>reversible</em> AES-256-GCM field encryption (with a
 *       per-record IV and an authentication tag). This, not password hashing, is
 *       the §2.5 encryption measure.</li>
 *   <li><b>Time stamping &amp; non-repudiation</b> - every register/login/logout
 *       and every mutating action is written, server-timestamped, to the
 *       append-only audit trail.</li>
 * </ul>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
package edu.cqu.drs.security;
