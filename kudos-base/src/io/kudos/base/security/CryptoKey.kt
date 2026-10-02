package io.kudos.base.security

/**
 * Default encryption key supplied explicitly by the operator. There is no built-in fallback.
 * Non-Spring applications may configure it programmatically, with `-Dkudos.crypto.default-key`,
 * or with `KUDOS_CRYPTO_DEFAULT_KEY`. Use at least 32 random characters from a secret manager.
 * Changing this key requires explicit migration of existing ciphertext with the old key first.
 */
object CryptoKey {
    @Volatile
    private var configuredKey: String? = null

    var KEY_DEFAULT: String
        get() = requireKey(configuredKey ?: System.getProperty("kudos.crypto.default-key")
            ?: System.getenv("KUDOS_CRYPTO_DEFAULT_KEY"))
        set(value) { configureDefaultKey(value) }

    fun configureDefaultKey(key: String) {
        configuredKey = requireKey(key)
    }

    internal fun requireKey(key: String?): String {
        check(!key.isNullOrBlank()) {
            "Missing encryption key: configure kudos.crypto.default-key or KUDOS_CRYPTO_DEFAULT_KEY"
        }
        require(key.length >= 32 && key != "io．Kudos．base.security ") {
            "Encryption key must contain at least 32 characters of externally generated key material"
        }
        return key
    }
}
