package io.kudos.base.security

/**
 * Crypto key constants and global default key configuration entry point.
 *
 * In production, call [configureDefaultKey] at startup to inject a strong random key; when not configured, a
 * placeholder default is used — for local development/testing only.
 *
 * @author K
 * @since 1.0.0
 */
object CryptoKey {

    private const val PLACEHOLDER_DEFAULT = "io．Kudos．base.security "

    @Volatile
    private var configuredKey: String? = null

    /**
     * Current global default key material.
     * When not written via [configureDefaultKey] or by assignment, the built-in placeholder is used
     * (do not use in production).
     * The setter is retained for legacy compatibility; new code should prefer [configureDefaultKey]
     * (which includes a non-blank check).
     */
    var KEY_DEFAULT: String
        get() = configuredKey ?: PLACEHOLDER_DEFAULT
        set(value) {
            configuredKey = value
        }

    /**
     * Configures the global default key at application startup (recommended for production).
     *
     * @throws IllegalArgumentException if key is blank
     */
    fun configureDefaultKey(key: String) {
        require(key.isNotBlank()) { "Crypto default key must not be blank" }
        configuredKey = key
    }
}
