package io.kudos.ms.user.core.account.security

/** Length-first default policy aligned with modern guidance; composition rules remain opt-in. */
open class DefaultPasswordPolicy(
    private val properties: PasswordPolicyProperties,
) : IPasswordPolicy {

    override fun violations(
        password: String,
        context: PasswordPolicyContext,
    ): Set<PasswordPolicyViolation> {
        if (!properties.enabled) return emptySet()
        val result = linkedSetOf<PasswordPolicyViolation>()
        val length = password.codePointCount(0, password.length)
        val minLength = properties.minLength.coerceIn(1, MAX_PASSWORD_LENGTH)
        val maxLength = properties.maxLength.coerceIn(minLength, MAX_PASSWORD_LENGTH)
        if (length < minLength) result += PasswordPolicyViolation.TOO_SHORT
        if (length > maxLength) result += PasswordPolicyViolation.TOO_LONG

        val normalized = password.lowercase()
        val username = context.username?.trim()?.lowercase().orEmpty()
        if (properties.rejectUsername && username.length >= MIN_USERNAME_CHECK_LENGTH && normalized.contains(username)) {
            result += PasswordPolicyViolation.USERNAME_INCLUDED
        }
        if (normalized in COMMON_PASSWORDS) result += PasswordPolicyViolation.COMMON_PASSWORD
        if (properties.rejectRepeatedCharacter && password.isNotEmpty() && password.all { it == password.first() }) {
            result += PasswordPolicyViolation.REPEATED_CHARACTER
        }
        if (properties.requireUppercase && password.none(Char::isUpperCase)) {
            result += PasswordPolicyViolation.UPPERCASE_REQUIRED
        }
        if (properties.requireLowercase && password.none(Char::isLowerCase)) {
            result += PasswordPolicyViolation.LOWERCASE_REQUIRED
        }
        if (properties.requireDigit && password.none(Char::isDigit)) {
            result += PasswordPolicyViolation.DIGIT_REQUIRED
        }
        if (properties.requireSpecial && password.none { !it.isLetterOrDigit() && !it.isWhitespace() }) {
            result += PasswordPolicyViolation.SPECIAL_REQUIRED
        }
        return result
    }

    private companion object {
        const val MAX_PASSWORD_LENGTH = 1024
        const val MIN_USERNAME_CHECK_LENGTH = 3
        val COMMON_PASSWORDS = setOf(
            "password",
            "password1",
            "password123",
            "12345678",
            "123456789",
            "1234567890",
            "qwerty123",
            "administrator",
            "letmein",
            "welcome",
        )
    }
}
