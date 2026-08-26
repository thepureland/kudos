package io.kudos.ability.security.common.support

import io.kudos.base.security.PasswordKit
import org.springframework.security.crypto.password.PasswordEncoder

/** Safe compatibility helpers for versioned password encodings and legacy bare BCrypt hashes. */
object PasswordEncodingKit {

    private val prefixedEncoding = Regex("^\\{[A-Za-z0-9._-]{1,32}}.+$")

    /** Whether the value can be delegated to the configured encoder or is a legacy BCrypt hash. */
    fun looksLikeEncodedPassword(encodedPassword: String?): Boolean =
        PasswordKit.looksLikeBcryptHash(encodedPassword) ||
            (!encodedPassword.isNullOrBlank() && prefixedEncoding.matches(encodedPassword))

    /** Match without propagating malformed/unknown encoding errors into an authentication flow. */
    fun matches(
        passwordEncoder: PasswordEncoder,
        rawPassword: CharSequence,
        encodedPassword: String?,
    ): Boolean {
        if (!looksLikeEncodedPassword(encodedPassword)) return false
        return runCatching {
            if (PasswordKit.looksLikeBcryptHash(encodedPassword)) {
                PasswordKit.matches(rawPassword.toString(), encodedPassword)
            } else {
                passwordEncoder.matches(rawPassword, encodedPassword)
            }
        }.getOrDefault(false)
    }

    /** Legacy values always need a prefix upgrade; versioned values defer to the encoder. */
    fun upgradeEncoding(passwordEncoder: PasswordEncoder, encodedPassword: String?): Boolean {
        if (PasswordKit.looksLikeBcryptHash(encodedPassword)) return true
        if (!looksLikeEncodedPassword(encodedPassword)) return false
        return runCatching { passwordEncoder.upgradeEncoding(encodedPassword) }.getOrDefault(false)
    }
}
