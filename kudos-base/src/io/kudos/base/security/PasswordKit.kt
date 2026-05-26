package io.kudos.base.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * Password hashing utility: BCrypt-based one-way hashing and verification.
 *
 * Use this for credentials that must be stored long-term, such as **user login passwords**. **Do not**
 * use [CryptoKit]'s AES to store passwords — AES is reversible, so a key leak exposes the entire table
 * in plaintext. BCrypt includes a random salt, is irreversible, and the cost factor can be raised via
 * [strength] as hardware improves without breaking verification of older hashes.
 *
 * Format:
 * - Hash output looks like `$2a$10$...` (60-character fixed length).
 * - Encodes algorithm version + cost + salt + hash — self-describing; [matches] does not require a separate salt.
 *
 * Thread safety: [BCryptPasswordEncoder] instances are themselves thread-safe; a singleton is kept here.
 *
 * @author K
 * @since 1.0.0
 */
object PasswordKit {

    /** BCrypt cost factor; 10 is a reasonable 2026 server-side default (~100ms/hash on a modern CPU). */
    const val DEFAULT_STRENGTH = 10

    private val DEFAULT_ENCODER = BCryptPasswordEncoder(DEFAULT_STRENGTH)

    /**
     * Computes the BCrypt hash of a plaintext password. Each call produces a different output (random salt).
     *
     * @param plainPassword plaintext password, must not be empty
     * @return BCrypt hash string (60 characters), ready to be persisted
     */
    fun hash(plainPassword: String): String {
        require(plainPassword.isNotEmpty()) { "Plaintext password must not be empty" }
        return DEFAULT_ENCODER.encode(plainPassword) ?: error("BCryptPasswordEncoder.encode returned null")
    }

    /**
     * Computes a hash using the specified cost factor. Should only be overridden explicitly in migration or test scenarios.
     */
    fun hash(plainPassword: String, strength: Int): String {
        require(plainPassword.isNotEmpty()) { "Plaintext password must not be empty" }
        return BCryptPasswordEncoder(strength).encode(plainPassword) ?: error("BCryptPasswordEncoder.encode returned null")
    }

    /**
     * Verifies whether a plaintext password matches the stored hash.
     *
     * @param plainPassword the plaintext password supplied by the user
     * @param storedHash the BCrypt hash string stored in the database
     * @return true if it matches; false on mismatch or invalid hash format (no exception is thrown)
     */
    fun matches(plainPassword: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrEmpty()) return false
        return try {
            DEFAULT_ENCODER.matches(plainPassword, storedHash)
        } catch (e: IllegalArgumentException) {
            // BCryptPasswordEncoder throws IAE on malformed hash strings; swallow it and return false so login flow can handle it uniformly
            false
        }
    }

    /**
     * Returns whether the given string looks like a BCrypt hash (used to distinguish legacy non-BCrypt data).
     */
    fun looksLikeBcryptHash(value: String?): Boolean {
        if (value == null) return false
        // BCrypt output: $2a$ / $2b$ / $2y$ prefix + cost + salt + hash, total length 60
        return value.length == 60 && value.matches(Regex("""^\$2[abxy]\$\d{2}\$.{53}$"""))
    }
}
