package io.kudos.ms.user.core.passport.security

/** Credential categories keep independent failure counters. */
enum class AuthenticationAttemptFactorEnum {
    PASSWORD,
    TOTP,
    RECOVERY_CODE,
    EMAIL_OTP,
}

/** Secret-free dimensions used to derive opaque attempt-counter keys. */
data class AuthenticationAttemptContext(
    val tenantId: String,
    val username: String,
    val loginIp: Long?,
)

enum class AuthenticationAttemptDecisionReasonEnum {
    REQUEST_RATE_LIMIT,
    PASSWORD_FAILURE_LIMIT,
    TOTP_FAILURE_LIMIT,
    RECOVERY_CODE_FAILURE_LIMIT,
    EMAIL_OTP_FAILURE_LIMIT,
    INFRASTRUCTURE_UNAVAILABLE,
}

data class AuthenticationAttemptDecision(
    val allowed: Boolean,
    val retryAfterSeconds: Long? = null,
    val reason: AuthenticationAttemptDecisionReasonEnum? = null,
) {
    companion object {
        val ALLOWED = AuthenticationAttemptDecision(allowed = true)
    }
}

/**
 * Authentication-specific limiter contract. Request traffic and each credential factor are
 * deliberately separate so a bad TOTP never consumes the password/account lock counter.
 */
interface IAuthenticationAttemptLimiter {
    fun consumeRequest(context: AuthenticationAttemptContext): AuthenticationAttemptDecision

    fun checkFailureLimit(
        context: AuthenticationAttemptContext,
        factor: AuthenticationAttemptFactorEnum,
    ): AuthenticationAttemptDecision

    fun recordFailure(
        context: AuthenticationAttemptContext,
        factor: AuthenticationAttemptFactorEnum,
    )

    fun clearFailures(
        context: AuthenticationAttemptContext,
        factors: Set<AuthenticationAttemptFactorEnum> = AuthenticationAttemptFactorEnum.entries.toSet(),
    )
}

object NoopAuthenticationAttemptLimiter : IAuthenticationAttemptLimiter {
    override fun consumeRequest(context: AuthenticationAttemptContext) = AuthenticationAttemptDecision.ALLOWED

    override fun checkFailureLimit(
        context: AuthenticationAttemptContext,
        factor: AuthenticationAttemptFactorEnum,
    ) = AuthenticationAttemptDecision.ALLOWED

    override fun recordFailure(
        context: AuthenticationAttemptContext,
        factor: AuthenticationAttemptFactorEnum,
    ) = Unit

    override fun clearFailures(
        context: AuthenticationAttemptContext,
        factors: Set<AuthenticationAttemptFactorEnum>,
    ) = Unit
}
