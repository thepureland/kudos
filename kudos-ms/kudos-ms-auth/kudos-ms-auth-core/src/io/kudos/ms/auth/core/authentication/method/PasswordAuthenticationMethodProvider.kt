package io.kudos.ms.auth.core.authentication.method

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.spi.AuthenticationChallenge
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodResult
import io.kudos.ms.auth.core.authentication.spi.IAuthenticationMethodProvider
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import io.kudos.ms.auth.core.authentication.mfa.recovery.service.iservice.IRecoveryCodeService
import io.kudos.ms.auth.core.authentication.mfa.AuthenticationSecondFactorRegistry
import io.kudos.ms.auth.core.authentication.mfa.policy.AuthenticationMfaPolicyEnforcer
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementOutcomeEnum
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.stereotype.Component

/** Transitional adapter that exposes the existing password + TOTP Passport flow through the new SPI. */
@Component
open class PasswordAuthenticationMethodProvider(
    private val passportService: IPassportService,
    private val recoveryCodeService: IRecoveryCodeService? = null,
    private val userAccountService: IUserAccountService? = null,
    private val mfaPolicyEnforcer: AuthenticationMfaPolicyEnforcer? = null,
    private val secondFactorRegistry: AuthenticationSecondFactorRegistry = AuthenticationSecondFactorRegistry(emptyList()),
) : IAuthenticationMethodProvider {

    override fun method(): String = METHOD_PASSWORD

    override fun begin(
        transaction: AuthenticationTransaction,
        request: AuthenticationTransactionCreateRequest,
    ): AuthenticationChallenge = if (transaction.tenantId == null) {
        AuthenticationChallenge(
            AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION,
            AuthenticationActionEnum.SELECT_TENANT,
        )
    } else {
        AuthenticationChallenge(
            AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
            AuthenticationActionEnum.VERIFY_PASSWORD,
        )
    }

    override fun verify(
        transaction: AuthenticationTransaction,
        action: AuthenticationActionEnum,
        request: AuthenticationActionRequest,
    ): AuthenticationMethodResult {
        val tenantId = transaction.tenantId
            ?: return retry(AuthenticationActionEnum.SELECT_TENANT, "TENANT_REQUIRED", request.username)
        val username = if (transaction.purpose == AuthenticationTransactionPurposeEnum.STEP_UP) {
            transaction.username
        } else {
            request.username?.trim()?.takeIf { it.isNotEmpty() } ?: transaction.username
        }
            ?: return retry(action, "USERNAME_REQUIRED", null)
        val password = request.plainPassword
            ?: return retry(action, "PASSWORD_REQUIRED", username)
        val authCode = if (action == AuthenticationActionEnum.VERIFY_TOTP) {
            request.code?.toLongOrNull()
                ?: return retry(AuthenticationActionEnum.VERIFY_TOTP, "INVALID_OTP_FORMAT", username)
        } else {
            null
        }
        val recoveryCode = if (action == AuthenticationActionEnum.VERIFY_RECOVERY_CODE) {
            request.code?.trim()?.takeIf(String::isNotEmpty)
                ?: return retry(AuthenticationActionEnum.VERIFY_RECOVERY_CODE, "INVALID_RECOVERY_CODE_FORMAT", username)
        } else {
            null
        }

        val result = passportService.login(
            PassportLoginRequest(
                tenantId = tenantId,
                username = username,
                plainPassword = password,
                loginIp = request.loginIp,
                authCode = authCode,
                recoveryCode = recoveryCode,
                loginDevice = request.loginDevice,
                loginBrowser = request.loginBrowser,
                loginOs = request.loginOs,
                userAgent = request.userAgent,
            )
        )
        return when (result.status) {
            PassportLoginStatusEnum.SUCCESS -> {
                val user = requireNotNull(result.userInfo)
                val amr = when (action) {
                    AuthenticationActionEnum.VERIFY_TOTP -> setOf(METHOD_PASSWORD, METHOD_TOTP)
                    AuthenticationActionEnum.VERIFY_RECOVERY_CODE -> setOf(METHOD_PASSWORD, METHOD_RECOVERY_CODE)
                    else -> setOf(METHOD_PASSWORD)
                }
                val success = AuthenticationMethodResult(
                    outcome = AuthenticationMethodOutcomeEnum.SUCCESS,
                    userId = user.id,
                    tenantId = user.tenantId,
                    username = user.username,
                    amr = amr,
                    acr = if (amr.size > 1) ACR_MFA else ACR_PASSWORD,
                )
                enforceMfaPolicy(transaction, success)
            }

            PassportLoginStatusEnum.OTP_REQUIRED -> {
                val account = findAccount(tenantId, username)
                val allowedMethods = account?.let {
                    runCatching {
                        mfaPolicyEnforcer?.enforce(
                            transaction.purpose,
                            tenantId,
                            it.id,
                            ACR_PASSWORD,
                        )?.decision?.policy?.allowedMethods
                    }.getOrNull()
                } ?: setOf(MfaMethodEnum.TOTP)
                retryWithMfaAlternatives(
                    errorCode = null,
                    username = username,
                    tenantId = tenantId,
                    verifiedUserId = account?.id,
                    allowedMethods = allowedMethods,
                    forceTotp = true,
                )
            }

            PassportLoginStatusEnum.WRONG_PASSWORD,
            PassportLoginStatusEnum.USER_NOT_FOUND,
            PassportLoginStatusEnum.INACTIVE,
            PassportLoginStatusEnum.LOCKED,
            PassportLoginStatusEnum.ACCOUNT_FROZEN,
            PassportLoginStatusEnum.INVALID_CREDENTIALS -> retry(
                AuthenticationActionEnum.VERIFY_PASSWORD,
                "INVALID_CREDENTIALS",
                username,
            )

            PassportLoginStatusEnum.OTP_WRONG -> retry(
                AuthenticationActionEnum.VERIFY_TOTP,
                "INVALID_OTP",
                username,
            )

            PassportLoginStatusEnum.RECOVERY_CODE_WRONG -> retryWithMfaAlternatives(
                "INVALID_RECOVERY_CODE",
                username,
                tenantId,
                verifiedUserId = transaction.userId,
            )

            PassportLoginStatusEnum.RATE_LIMITED -> retry(
                action,
                "TOO_MANY_AUTHENTICATION_ATTEMPTS",
                username,
            )

        }
    }

    private fun retry(
        action: AuthenticationActionEnum,
        errorCode: String?,
        username: String?,
    ) = AuthenticationMethodResult(
        outcome = AuthenticationMethodOutcomeEnum.CHALLENGE,
        nextAction = action,
        username = username,
        errorCode = errorCode,
    )

    private fun retryWithMfaAlternatives(
        errorCode: String?,
        username: String,
        tenantId: String,
        verifiedUserId: String? = null,
        allowedMethods: Set<MfaMethodEnum> = setOf(MfaMethodEnum.TOTP),
        forceTotp: Boolean = true,
    ): AuthenticationMethodResult {
        val recoveryAllowed = runCatching {
            mfaPolicyEnforcer?.recoveryCodesEnabled(tenantId) ?: true
        }.getOrDefault(false)
        val account = findAccount(tenantId, username)
        val userId = verifiedUserId ?: account?.id
        val hasRecoveryCodes = if (recoveryAllowed && userId != null) {
            runCatching {
                recoveryCodeService?.status(userId, tenantId)?.remaining ?: 0
            }.getOrDefault(0)
        } else {
            0
        }
        val actions = buildSet {
            val totpAvailable = forceTotp || account?.authenticationKey?.isNotBlank() == true
            if (MfaMethodEnum.TOTP in allowedMethods && totpAvailable) {
                add(AuthenticationActionEnum.VERIFY_TOTP)
                if (hasRecoveryCodes > 0) add(AuthenticationActionEnum.VERIFY_RECOVERY_CODE)
            }
            if (userId != null) {
                addAll(secondFactorRegistry.availableActions(userId, tenantId, allowedMethods))
            }
        }
        if (actions.isEmpty()) {
            return AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.FAILURE,
                terminal = true,
                username = username,
                errorCode = errorCode ?: "MFA_REQUIRED",
            )
        }
        return AuthenticationMethodResult(
            outcome = AuthenticationMethodOutcomeEnum.CHALLENGE,
            nextAction = actions.first(),
            nextActions = actions,
            userId = userId,
            tenantId = tenantId,
            username = username,
            amr = if (userId != null) setOf(METHOD_PASSWORD) else emptySet(),
            acr = if (userId != null) ACR_PASSWORD else null,
            errorCode = errorCode,
        )
    }

    private fun enforceMfaPolicy(
        transaction: AuthenticationTransaction,
        success: AuthenticationMethodResult,
    ): AuthenticationMethodResult {
        val enforcer = mfaPolicyEnforcer ?: return success
        val userId = requireNotNull(success.userId)
        val tenantId = requireNotNull(success.tenantId ?: transaction.tenantId)
        val acr = requireNotNull(success.acr)
        val enforcement = enforcer.enforce(transaction.purpose, tenantId, userId, acr)
        return when (enforcement.outcome) {
            MfaPolicyEnforcementOutcomeEnum.ALLOW -> success
            MfaPolicyEnforcementOutcomeEnum.ALLOW_ENROLLMENT_REQUIRED -> success.copy(
                postAuthenticationActions = setOf(AuthenticationActionEnum.ENROLL_MFA),
            )
            MfaPolicyEnforcementOutcomeEnum.DENY_ENROLLMENT_REQUIRED -> AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.FAILURE,
                terminal = true,
                username = success.username,
                errorCode = "MFA_ENROLLMENT_REQUIRED",
            )
            MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR -> retryWithMfaAlternatives(
                errorCode = "MFA_REQUIRED",
                username = requireNotNull(success.username),
                tenantId = tenantId,
                verifiedUserId = userId,
                allowedMethods = enforcement.decision?.policy?.allowedMethods ?: setOf(MfaMethodEnum.TOTP),
                forceTotp = false,
            )
        }
    }

    private fun findAccount(tenantId: String, username: String) = runCatching {
        userAccountService?.getUserByTenantIdAndUsername(tenantId, username)
    }.getOrNull()

    companion object {
        const val METHOD_PASSWORD = "password"
        const val METHOD_TOTP = "totp"
        const val METHOD_RECOVERY_CODE = "recovery_code"
        const val ACR_PASSWORD = "urn:kudos:acr:password"
        const val ACR_MFA = "urn:kudos:acr:mfa"
    }
}
