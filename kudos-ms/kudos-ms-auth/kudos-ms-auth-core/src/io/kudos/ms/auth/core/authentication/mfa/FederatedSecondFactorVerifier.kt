package io.kudos.ms.auth.core.authentication.mfa

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.core.authentication.mfa.recovery.service.iservice.IRecoveryCodeService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptContext
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptFactorEnum
import io.kudos.ms.user.core.passport.security.IAuthenticationAttemptLimiter
import io.kudos.ms.user.core.passport.security.NoopAuthenticationAttemptLimiter
import org.springframework.stereotype.Component

data class SecondFactorVerificationResult(
    val success: Boolean,
    val method: String? = null,
    val acr: String? = null,
    val errorCode: String? = null,
)

/** Verifies a local second factor after an already validated federated primary identity. */
@Component
open class FederatedSecondFactorVerifier(
    private val userAccountService: IUserAccountService,
    private val recoveryCodeService: IRecoveryCodeService,
    private val attemptLimiter: IAuthenticationAttemptLimiter = NoopAuthenticationAttemptLimiter,
) {

    open fun availableActions(userId: String, tenantId: String): Set<AuthenticationActionEnum> = buildSet {
        add(AuthenticationActionEnum.VERIFY_TOTP)
        val remaining = runCatching { recoveryCodeService.status(userId, tenantId).remaining }.getOrDefault(0)
        if (remaining > 0) add(AuthenticationActionEnum.VERIFY_RECOVERY_CODE)
    }

    open fun verify(
        userId: String,
        tenantId: String,
        username: String,
        action: AuthenticationActionEnum,
        request: AuthenticationActionRequest,
    ): SecondFactorVerificationResult {
        val factor = when (action) {
            AuthenticationActionEnum.VERIFY_TOTP -> AuthenticationAttemptFactorEnum.TOTP
            AuthenticationActionEnum.VERIFY_RECOVERY_CODE -> AuthenticationAttemptFactorEnum.RECOVERY_CODE
            else -> return SecondFactorVerificationResult(false, errorCode = "UNSUPPORTED_SECOND_FACTOR")
        }
        val context = AuthenticationAttemptContext(tenantId, username, request.loginIp)
        if (!attemptLimiter.checkFailureLimit(context, factor).allowed) {
            return SecondFactorVerificationResult(false, errorCode = "TOO_MANY_AUTHENTICATION_ATTEMPTS")
        }
        val success = when (action) {
            AuthenticationActionEnum.VERIFY_TOTP -> {
                val code = request.code?.toLongOrNull()?.takeIf { it in 0..999_999 }
                    ?: return failed(context, factor, "INVALID_OTP_FORMAT")
                userAccountService.verifyAuthCode(userId, code)
            }
            AuthenticationActionEnum.VERIFY_RECOVERY_CODE -> {
                val code = request.code?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return failed(context, factor, "INVALID_RECOVERY_CODE_FORMAT")
                recoveryCodeService.consume(userId, tenantId, code)
            }
        }
        if (!success) {
            return failed(
                context,
                factor,
                if (factor == AuthenticationAttemptFactorEnum.TOTP) "INVALID_OTP" else "INVALID_RECOVERY_CODE",
            )
        }
        attemptLimiter.clearFailures(context, setOf(factor))
        return SecondFactorVerificationResult(
            success = true,
            method = if (factor == AuthenticationAttemptFactorEnum.TOTP) METHOD_TOTP else METHOD_RECOVERY_CODE,
        )
    }

    private fun failed(
        context: AuthenticationAttemptContext,
        factor: AuthenticationAttemptFactorEnum,
        errorCode: String,
    ): SecondFactorVerificationResult {
        attemptLimiter.recordFailure(context, factor)
        return SecondFactorVerificationResult(false, errorCode = errorCode)
    }

    companion object {
        const val METHOD_TOTP = "totp"
        const val METHOD_RECOVERY_CODE = "recovery_code"
    }
}
