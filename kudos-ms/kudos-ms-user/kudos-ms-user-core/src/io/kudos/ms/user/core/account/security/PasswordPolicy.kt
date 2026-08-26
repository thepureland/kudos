package io.kudos.ms.user.core.account.security

enum class PasswordPurpose {
    LOGIN,
    SECURITY,
}

data class PasswordPolicyContext(
    val purpose: PasswordPurpose,
    val userId: String? = null,
    val username: String? = null,
    val tenantId: String? = null,
)

enum class PasswordPolicyViolation {
    TOO_SHORT,
    TOO_LONG,
    USERNAME_INCLUDED,
    COMMON_PASSWORD,
    REPEATED_CHARACTER,
    UPPERCASE_REQUIRED,
    LOWERCASE_REQUIRED,
    DIGIT_REQUIRED,
    SPECIAL_REQUIRED,
}

/** Application-replaceable password validation boundary. */
fun interface IPasswordPolicy {
    fun violations(password: String, context: PasswordPolicyContext): Set<PasswordPolicyViolation>
}

class PasswordPolicyException(
    val violations: Set<PasswordPolicyViolation>,
) : IllegalArgumentException("Password policy violation: ${violations.joinToString(",")}")

/** Stable failure used when the candidate matches the current or a retained historical password. */
class PasswordReusedException : IllegalArgumentException("Password must not reuse a current or historical value")
