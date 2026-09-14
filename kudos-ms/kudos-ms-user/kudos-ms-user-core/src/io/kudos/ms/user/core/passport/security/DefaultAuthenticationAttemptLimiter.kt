package io.kudos.ms.user.core.passport.security

import io.kudos.base.logger.LogFactory
import java.security.MessageDigest
import java.util.Locale

/** Default authentication limiter using opaque, non-enumerable counter keys. */
open class DefaultAuthenticationAttemptLimiter(
    private val store: IAuthenticationAttemptStore,
    private val properties: AuthenticationAttemptLimitProperties,
) : IAuthenticationAttemptLimiter {
    private val log = LogFactory.getLog(this::class)

    override fun consumeRequest(context: AuthenticationAttemptContext): AuthenticationAttemptDecision {
        if (!properties.enabled) return AuthenticationAttemptDecision.ALLOWED
        val buckets = buildList {
            enabledBucket(
                "request-principal",
                principal(context),
                properties.principalMaxAttempts,
                properties.principalWindowSeconds,
            )?.let(::add)
            context.loginIp?.let { ip ->
                enabledBucket(
                    "request-ip",
                    ip.toString(),
                    properties.ipMaxAttempts,
                    properties.ipWindowSeconds,
                )?.let(::add)
            }
        }
        return execute(AuthenticationAttemptDecisionReasonEnum.REQUEST_RATE_LIMIT) { store.consume(buckets) }
    }

    override fun checkFailureLimit(
        context: AuthenticationAttemptContext,
        factor: AuthenticationAttemptFactorEnum,
    ): AuthenticationAttemptDecision {
        if (!properties.enabled) return AuthenticationAttemptDecision.ALLOWED
        val bucket = failureBucket(context, factor) ?: return AuthenticationAttemptDecision.ALLOWED
        return execute(reason(factor)) { store.inspect(listOf(bucket)) }
    }

    override fun recordFailure(
        context: AuthenticationAttemptContext,
        factor: AuthenticationAttemptFactorEnum,
    ) {
        if (!properties.enabled) return
        val bucket = failureBucket(context, factor) ?: return
        execute(reason(factor)) { store.consume(listOf(bucket)) }
    }

    override fun clearFailures(
        context: AuthenticationAttemptContext,
        factors: Set<AuthenticationAttemptFactorEnum>,
    ) {
        if (!properties.enabled) return
        try {
            store.clear(factors.mapNotNull { failureBucket(context, it)?.key })
        } catch (e: Exception) {
            log.error(e, "Authentication attempt store clear failed")
        }
    }

    private fun failureBucket(
        context: AuthenticationAttemptContext,
        factor: AuthenticationAttemptFactorEnum,
    ): AuthenticationAttemptBucket? = when (factor) {
        AuthenticationAttemptFactorEnum.PASSWORD -> enabledBucket(
            "failure-password",
            principal(context),
            properties.passwordFailureMaxAttempts,
            properties.passwordFailureWindowSeconds,
        )

        AuthenticationAttemptFactorEnum.TOTP -> enabledBucket(
            "failure-totp",
            principal(context),
            properties.totpFailureMaxAttempts,
            properties.totpFailureWindowSeconds,
        )

        AuthenticationAttemptFactorEnum.RECOVERY_CODE -> enabledBucket(
            "failure-recovery-code",
            principal(context),
            properties.recoveryCodeFailureMaxAttempts,
            properties.recoveryCodeFailureWindowSeconds,
        )

        AuthenticationAttemptFactorEnum.EMAIL_OTP -> enabledBucket(
            "failure-email-otp",
            principal(context),
            properties.emailOtpFailureMaxAttempts,
            properties.emailOtpFailureWindowSeconds,
        )
    }

    private fun enabledBucket(
        scope: String,
        dimension: String,
        maxAttempts: Int,
        windowSeconds: Long,
    ): AuthenticationAttemptBucket? {
        if (maxAttempts <= 0) return null
        return AuthenticationAttemptBucket(
            key = "$KEY_PREFIX$scope:${digest("$scope\u0000$dimension")}",
            maxAttempts = maxAttempts,
            windowSeconds = windowSeconds,
        )
    }

    private fun principal(context: AuthenticationAttemptContext): String =
        context.tenantId.trim().lowercase(Locale.ROOT) + "\u0000" +
            context.username.trim().lowercase(Locale.ROOT)

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun execute(
        blockedReason: AuthenticationAttemptDecisionReasonEnum,
        operation: () -> AuthenticationAttemptStoreDecision,
    ): AuthenticationAttemptDecision = try {
        val decision = operation()
        if (decision.allowed) AuthenticationAttemptDecision.ALLOWED else AuthenticationAttemptDecision(
            allowed = false,
            retryAfterSeconds = decision.retryAfterSeconds,
            reason = blockedReason,
        )
    } catch (e: Exception) {
        log.error(e, "Authentication attempt store unavailable")
        if (properties.failOpen) AuthenticationAttemptDecision.ALLOWED else AuthenticationAttemptDecision(
            allowed = false,
            retryAfterSeconds = 1,
            reason = AuthenticationAttemptDecisionReasonEnum.INFRASTRUCTURE_UNAVAILABLE,
        )
    }

    private fun reason(factor: AuthenticationAttemptFactorEnum) = when (factor) {
        AuthenticationAttemptFactorEnum.PASSWORD -> AuthenticationAttemptDecisionReasonEnum.PASSWORD_FAILURE_LIMIT
        AuthenticationAttemptFactorEnum.TOTP -> AuthenticationAttemptDecisionReasonEnum.TOTP_FAILURE_LIMIT
        AuthenticationAttemptFactorEnum.RECOVERY_CODE ->
            AuthenticationAttemptDecisionReasonEnum.RECOVERY_CODE_FAILURE_LIMIT
        AuthenticationAttemptFactorEnum.EMAIL_OTP ->
            AuthenticationAttemptDecisionReasonEnum.EMAIL_OTP_FAILURE_LIMIT
    }

    private companion object {
        const val KEY_PREFIX = "kudos:auth:attempt:"
    }
}
