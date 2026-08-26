package io.kudos.ms.auth.provider.webauthn.authentication

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.AuthenticationMfaPolicyEnforcer
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementOutcomeEnum
import io.kudos.ms.auth.core.authentication.mfa.IAuthenticationSecondFactorProvider
import io.kudos.ms.auth.core.authentication.mfa.SecondFactorVerificationResult
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.auth.core.authentication.spi.AuthenticationChallenge
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodResult
import io.kudos.ms.auth.core.authentication.spi.IAuthenticationMethodProvider
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionFinishCommand
import io.kudos.ms.auth.provider.webauthn.service.WebAuthnAssertionService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/** Adapts verified Passkey assertions to the method-neutral authentication transaction SPI. */
@Component
@ConditionalOnProperty(prefix = "kudos.ms.auth.webauthn", name = ["enabled"], havingValue = "true")
open class WebAuthnAuthenticationMethodProvider(
    private val assertionService: WebAuthnAssertionService,
    private val mfaPolicyEnforcer: AuthenticationMfaPolicyEnforcer? = null,
    private val credentialService: IWebAuthnCredentialService? = null,
) : IAuthenticationMethodProvider, IAuthenticationSecondFactorProvider {

    override fun method(): String = METHOD_PASSKEY

    override fun supports(transaction: AuthenticationTransaction): Boolean =
        transaction.purpose in setOf(
            AuthenticationTransactionPurposeEnum.LOGIN,
            AuthenticationTransactionPurposeEnum.STEP_UP,
        )

    override fun action(): AuthenticationActionEnum = AuthenticationActionEnum.VERIFY_PASSKEY

    override fun policyMethod(): MfaMethodEnum = MfaMethodEnum.WEBAUTHN

    override fun isAvailable(userId: String, tenantId: String): Boolean =
        credentialService?.isEnrolled(userId, tenantId) == true

    override fun verify(
        transactionId: String,
        userId: String,
        tenantId: String,
        request: AuthenticationActionRequest,
    ): SecondFactorVerificationResult {
        val command = assertionCommand(request)
            ?: return SecondFactorVerificationResult(false, errorCode = "INVALID_PASSKEY")
        val assertion = runCatching {
            assertionService.finish(tenantId, command, userId, transactionId)
        }.getOrElse { return SecondFactorVerificationResult(false, errorCode = "INVALID_PASSKEY") }
        if (assertion.userId != userId) {
            return SecondFactorVerificationResult(false, errorCode = "INVALID_PASSKEY")
        }
        return SecondFactorVerificationResult(
            success = true,
            method = METHOD_WEBAUTHN,
            acr = if (assertion.userVerified) {
                DefaultAuthenticationAssurancePolicy.ACR_PHISHING_RESISTANT
            } else {
                DefaultAuthenticationAssurancePolicy.ACR_MFA
            },
        )
    }

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
            AuthenticationActionEnum.VERIFY_PASSKEY,
        )
    }

    override fun verify(
        transaction: AuthenticationTransaction,
        action: AuthenticationActionEnum,
        request: AuthenticationActionRequest,
    ): AuthenticationMethodResult {
        if (action != AuthenticationActionEnum.VERIFY_PASSKEY) return terminalFailure("PASSKEY_ACTION_INVALID")
        val tenantId = transaction.tenantId ?: return challenge("TENANT_REQUIRED")
        val ceremonyId = request.attributes[ATTRIBUTE_CEREMONY_ID]
            ?.trim()?.takeIf(String::isNotEmpty)
            ?: return challenge("PASSKEY_CEREMONY_REQUIRED")
        val responseJson = request.attributes[ATTRIBUTE_CREDENTIAL_RESPONSE]
            ?.takeIf(String::isNotBlank)
            ?: return challenge("PASSKEY_RESPONSE_REQUIRED")
        val expectedUserId = if (transaction.purpose == AuthenticationTransactionPurposeEnum.STEP_UP) {
            transaction.initiatorUserId ?: return terminalFailure("STEP_UP_SUBJECT_REQUIRED")
        } else {
            null
        }
        val assertion = runCatching {
            assertionService.finish(
                tenantId = tenantId,
                command = WebAuthnAssertionFinishCommand(ceremonyId, responseJson),
                expectedUserId = expectedUserId,
                expectedBindingId = transaction.id,
            )
        }.getOrElse { return challenge("INVALID_PASSKEY") }
        val success = AuthenticationMethodResult(
            outcome = AuthenticationMethodOutcomeEnum.SUCCESS,
            userId = assertion.userId,
            tenantId = tenantId,
            username = assertion.username,
            amr = setOf(METHOD_PASSKEY, METHOD_WEBAUTHN),
            acr = if (assertion.userVerified) {
                DefaultAuthenticationAssurancePolicy.ACR_PHISHING_RESISTANT
            } else {
                DefaultAuthenticationAssurancePolicy.ACR_WEBAUTHN
            },
        )
        return enforceMfaPolicy(transaction, success)
    }

    private fun enforceMfaPolicy(
        transaction: AuthenticationTransaction,
        success: AuthenticationMethodResult,
    ): AuthenticationMethodResult {
        val enforcement = mfaPolicyEnforcer?.enforce(
            transaction.purpose,
            requireNotNull(success.tenantId),
            requireNotNull(success.userId),
            requireNotNull(success.acr),
        ) ?: return success
        return when (enforcement.outcome) {
            MfaPolicyEnforcementOutcomeEnum.ALLOW -> success
            MfaPolicyEnforcementOutcomeEnum.ALLOW_ENROLLMENT_REQUIRED -> success.copy(
                postAuthenticationActions = setOf(AuthenticationActionEnum.ENROLL_MFA),
            )
            MfaPolicyEnforcementOutcomeEnum.DENY_ENROLLMENT_REQUIRED ->
                terminalFailure("MFA_ENROLLMENT_REQUIRED")
            MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR -> terminalFailure("MFA_REQUIRED")
        }
    }

    private fun challenge(errorCode: String) = AuthenticationMethodResult(
        outcome = AuthenticationMethodOutcomeEnum.CHALLENGE,
        nextAction = AuthenticationActionEnum.VERIFY_PASSKEY,
        errorCode = errorCode,
    )

    private fun terminalFailure(errorCode: String) = AuthenticationMethodResult(
        outcome = AuthenticationMethodOutcomeEnum.FAILURE,
        terminal = true,
        errorCode = errorCode,
    )

    private fun assertionCommand(request: AuthenticationActionRequest): WebAuthnAssertionFinishCommand? {
        val ceremonyId = request.attributes[ATTRIBUTE_CEREMONY_ID]
            ?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val responseJson = request.attributes[ATTRIBUTE_CREDENTIAL_RESPONSE]
            ?.takeIf(String::isNotBlank) ?: return null
        return WebAuthnAssertionFinishCommand(ceremonyId, responseJson)
    }

    companion object {
        const val METHOD_PASSKEY = "passkey"
        const val METHOD_WEBAUTHN = "webauthn"
        const val ATTRIBUTE_CEREMONY_ID = "webauthnCeremonyId"
        const val ATTRIBUTE_CREDENTIAL_RESPONSE = "webauthnCredentialResponseJson"
    }
}
