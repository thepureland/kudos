package io.kudos.ms.auth.provider.webauthn.service

import com.yubico.webauthn.AssertionRequest
import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartAssertionOptions
import com.yubico.webauthn.data.ByteArray as YubicoByteArray
import com.yubico.webauthn.data.RelyingPartyIdentity
import com.yubico.webauthn.data.UserVerificationRequirement
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAuthenticatorRiskService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialFingerprints
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.VerifiedWebAuthnAssertion
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskEnforcementCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.service.iservice.IWebAuthnAuthenticatorRiskPolicyService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.auth.provider.webauthn.ValidatedWebAuthnProviderConfiguration
import io.kudos.ms.auth.provider.webauthn.WebAuthnProviderConfiguration
import io.kudos.ms.auth.provider.webauthn.WebAuthnUserHandles
import io.kudos.ms.auth.provider.webauthn.ceremony.IWebAuthnCeremonyStore
import io.kudos.ms.auth.provider.webauthn.ceremony.WebAuthnCeremonyState
import io.kudos.ms.auth.provider.webauthn.ceremony.WebAuthnCeremonyTypeEnum
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionFinish
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionFinishCommand
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnAssertionStart
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import io.kudos.ms.auth.provider.webauthn.protocol.IWebAuthnAssertionVerifier
import io.kudos.ms.auth.provider.webauthn.protocol.TenantWebAuthnCredentialRepository
import io.kudos.ms.auth.provider.webauthn.protocol.WebAuthnAssertionVerificationRequest
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.Optional

/** Orchestrates known-user and username-less WebAuthn authentication ceremonies. */
@Service
@ConditionalOnProperty(prefix = "kudos.ms.auth.webauthn", name = ["enabled"], havingValue = "true")
open class WebAuthnAssertionService(
    private val userAccountService: IUserAccountService,
    private val credentialService: IWebAuthnCredentialService,
    private val ceremonyStore: IWebAuthnCeremonyStore,
    private val assertionVerifier: IWebAuthnAssertionVerifier,
    private val authenticatorRiskService: IWebAuthnAuthenticatorRiskService,
    private val authenticatorRiskPolicyService: IWebAuthnAuthenticatorRiskPolicyService,
    private val providerConfiguration: WebAuthnProviderConfiguration,
    private val clock: Clock = Clock.systemUTC(),
    private val secureRandom: SecureRandom = SecureRandom(),
) {

    open fun begin(
        tenantId: String,
        userId: String? = null,
        bindingId: String? = null,
    ): WebAuthnAssertionStart {
        requireScope(tenantId, userId, bindingId)
        val configuration = providerConfiguration.validated()
        val account = userId?.let { activeAccount(it, tenantId) }
        val userHandle = userId?.let { WebAuthnUserHandles.derive(tenantId, it) }
        val repository = repository(tenantId, account, userHandle)
        val options = StartAssertionOptions.builder()
            .userVerification(configuration.userVerification)
            .timeout(configuration.browserTimeoutMillis)
            .apply {
                if (account != null && userHandle != null) {
                    username(account.username)
                }
            }
            .build()
        val request = relyingParty(configuration, repository).startAssertion(options)
        val now = clock.instant()
        val expiresAt = now.plusSeconds(configuration.ceremonyTtlSeconds)
        repeat(MAX_ID_GENERATION_ATTEMPTS) {
            val id = randomId()
            if (ceremonyStore.create(
                    WebAuthnCeremonyState(
                        id = id,
                        type = WebAuthnCeremonyTypeEnum.ASSERTION,
                        tenantId = tenantId,
                        userId = userId,
                        bindingId = bindingId,
                        requestJson = request.toJson(),
                        createdAt = now,
                        expiresAt = expiresAt,
                    )
                )
            ) {
                return WebAuthnAssertionStart(id, request.toCredentialsGetJson(), expiresAt)
            }
        }
        fail("WEBAUTHN_CEREMONY_ID_EXHAUSTED")
    }

    open fun finish(
        tenantId: String,
        command: WebAuthnAssertionFinishCommand,
        expectedUserId: String? = null,
        expectedBindingId: String? = null,
    ): WebAuthnAssertionFinish {
        requireScope(tenantId, expectedUserId, expectedBindingId)
        validateFinishInput(command)
        val state = ceremonyStore.consume(command.ceremonyId)
            ?.takeIf {
                it.type == WebAuthnCeremonyTypeEnum.ASSERTION &&
                    it.tenantId == tenantId &&
                    (expectedUserId == null || it.userId == expectedUserId) &&
                    (expectedBindingId == null || it.bindingId == expectedBindingId)
            }
            ?: fail("WEBAUTHN_CEREMONY_INVALID")
        val configuration = providerConfiguration.validated()
        val request = runCatching { AssertionRequest.fromJson(state.requestJson) }
            .getOrElse { fail("WEBAUTHN_CEREMONY_INVALID", it) }
        if (request.publicKeyCredentialRequestOptions.rpId != configuration.rpId) {
            fail("WEBAUTHN_CEREMONY_INVALID")
        }
        val knownAccount = state.userId?.let { activeAccount(it, tenantId) }
        val knownUserHandle = state.userId?.let { WebAuthnUserHandles.derive(tenantId, it) }
        validateRequestSubject(request, knownAccount, knownUserHandle)
        val repository = repository(tenantId, knownAccount, knownUserHandle)
        val verified = runCatching {
            assertionVerifier.verify(
                WebAuthnAssertionVerificationRequest(
                    request = request,
                    credentialResponseJson = command.credentialResponseJson,
                    rpId = configuration.rpId,
                    rpName = configuration.rpName,
                    origins = configuration.origins,
                    credentialRepository = repository,
                    clock = clock,
                )
            )
        }.getOrElse { fail("WEBAUTHN_ASSERTION_VERIFICATION_FAILED", it) }
        if (configuration.userVerification == UserVerificationRequirement.REQUIRED && !verified.userVerified) {
            fail("WEBAUTHN_USER_VERIFICATION_REQUIRED")
        }
        val credential = credentialService.findActive(tenantId, verified.credentialId)
            ?: fail("WEBAUTHN_CREDENTIAL_NOT_FOUND")
        if (credential.userHandle != verified.userHandle) fail("WEBAUTHN_CREDENTIAL_SUBJECT_MISMATCH")
        val resolvedUserId = credential.userId
        if (state.userId?.let { it != resolvedUserId } == true ||
            expectedUserId?.let { it != resolvedUserId } == true
        ) {
            fail("WEBAUTHN_CREDENTIAL_SUBJECT_MISMATCH")
        }
        val account = activeAccount(resolvedUserId, tenantId)
        if (account.username != verified.username) fail("WEBAUTHN_CREDENTIAL_SUBJECT_MISMATCH")
        val risk = authenticatorRiskService.evaluate(credential.aaguid)
        try {
            authenticatorRiskPolicyService.enforce(
                WebAuthnAuthenticatorRiskEnforcementCommand(
                    tenantId = tenantId,
                    userId = resolvedUserId,
                    credentialIdFingerprint = runCatching {
                        WebAuthnCredentialFingerprints.sha256(verified.credentialId)
                    }.getOrElse { fail("WEBAUTHN_CREDENTIAL_ID_INVALID", it) },
                    risk = risk,
                )
            )
        } catch (e: WebAuthnAuthenticatorRiskPolicyException) {
            fail(e.errorCode, e)
        }
        val updated = credentialService.recordVerifiedAssertion(
            VerifiedWebAuthnAssertion(
                tenantId = tenantId,
                userId = resolvedUserId,
                credentialId = verified.credentialId,
                signatureCount = verified.signatureCount,
                backedUp = verified.backedUp,
            )
        )
        return WebAuthnAssertionFinish(
            userId = resolvedUserId,
            username = account.username,
            credential = updated,
            userVerified = verified.userVerified,
            backupEligible = verified.backupEligible,
            backedUp = verified.backedUp,
        )
    }

    private fun repository(
        tenantId: String,
        account: UserAccount?,
        userHandle: YubicoByteArray?,
    ): TenantWebAuthnCredentialRepository = TenantWebAuthnCredentialRepository(
        tenantId = tenantId,
        credentialService = credentialService,
        userAccountService = userAccountService,
        knownUsername = account?.username,
        knownUserHandle = userHandle,
        knownCredentialIds = account?.let {
            credentialService.listActive(tenantId, it.id).map { summary -> summary.credentialId }.toSet()
        } ?: emptySet(),
    )

    private fun validateRequestSubject(
        request: AssertionRequest,
        account: UserAccount?,
        userHandle: YubicoByteArray?,
    ) {
        val username = request.username
        val requestedHandle = request.userHandle
        if (account == null) {
            if (username.isPresent || requestedHandle.isPresent) fail("WEBAUTHN_CEREMONY_INVALID")
        } else if (username != Optional.of(account.username) || requestedHandle.isPresent || userHandle == null) {
            fail("WEBAUTHN_CEREMONY_INVALID")
        }
    }

    private fun validateFinishInput(command: WebAuthnAssertionFinishCommand) {
        if (!CEREMONY_ID.matches(command.ceremonyId)) fail("WEBAUTHN_CEREMONY_INVALID")
        if (command.credentialResponseJson.isBlank() || command.credentialResponseJson.length > MAX_RESPONSE_JSON_CHARS) {
            fail("WEBAUTHN_ASSERTION_RESPONSE_INVALID")
        }
    }

    private fun requireScope(tenantId: String, userId: String?, bindingId: String?) {
        if (tenantId.isBlank() || tenantId.length > 36 ||
            userId?.let { it.isBlank() || it.length > 36 } == true ||
            bindingId?.let { it.isBlank() || it.length > 64 } == true
        ) {
            fail("WEBAUTHN_SCOPE_INVALID")
        }
    }

    private fun activeAccount(userId: String, tenantId: String): UserAccount =
        userAccountService.get(userId)
            ?.takeIf { it.tenantId == tenantId && it.active }
            ?: fail("WEBAUTHN_ACCOUNT_NOT_FOUND")

    private fun relyingParty(
        configuration: ValidatedWebAuthnProviderConfiguration,
        repository: CredentialRepository,
    ): RelyingParty = RelyingParty.builder()
        .identity(
            RelyingPartyIdentity.builder()
                .id(configuration.rpId)
                .name(configuration.rpName)
                .build()
        )
        .credentialRepository(repository)
        .origins(configuration.origins)
        .clock(clock)
        .build()

    private fun randomId(): String = ByteArray(32).also(secureRandom::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw WebAuthnProviderException(errorCode, cause)

    private companion object {
        const val MAX_ID_GENERATION_ATTEMPTS = 3
        const val MAX_RESPONSE_JSON_CHARS = 65_536
        val CEREMONY_ID = Regex("^[A-Za-z0-9_-]{43}$")
    }
}
