package io.kudos.ms.auth.provider.webauthn.service

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria
import com.yubico.webauthn.data.AttestationConveyancePreference
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import com.yubico.webauthn.data.RelyingPartyIdentity
import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.data.RegistrationExtensionInputs
import com.yubico.webauthn.data.UserIdentity
import com.yubico.webauthn.data.UserVerificationRequirement
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAaguidPolicyModeEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAttestationPolicyService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.VerifiedWebAuthnCredentialRegistration
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.auth.provider.webauthn.ValidatedWebAuthnProviderConfiguration
import io.kudos.ms.auth.provider.webauthn.WebAuthnProviderConfiguration
import io.kudos.ms.auth.provider.webauthn.WebAuthnUserHandles
import io.kudos.ms.auth.provider.webauthn.ceremony.IWebAuthnCeremonyStore
import io.kudos.ms.auth.provider.webauthn.ceremony.WebAuthnCeremonyState
import io.kudos.ms.auth.provider.webauthn.ceremony.WebAuthnCeremonyTypeEnum
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationFinish
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationFinishCommand
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnRegistrationStart
import io.kudos.ms.auth.provider.webauthn.protocol.IWebAuthnRegistrationVerifier
import io.kudos.ms.auth.provider.webauthn.protocol.TenantWebAuthnCredentialRepository
import io.kudos.ms.auth.provider.webauthn.protocol.WebAuthnRegistrationVerificationRequest
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64

/** Orchestrates registration while keeping browser data behind the protocol-verification boundary. */
@Service
@ConditionalOnProperty(prefix = "kudos.ms.auth.webauthn", name = ["enabled"], havingValue = "true")
open class WebAuthnRegistrationService(
    private val userAccountService: IUserAccountService,
    private val credentialService: IWebAuthnCredentialService,
    private val ceremonyStore: IWebAuthnCeremonyStore,
    private val registrationVerifier: IWebAuthnRegistrationVerifier,
    private val providerConfiguration: WebAuthnProviderConfiguration,
    private val clock: Clock = Clock.systemUTC(),
    private val secureRandom: SecureRandom = SecureRandom(),
    private val attestationPolicyService: IWebAuthnAttestationPolicyService? = null,
) {

    open fun begin(userId: String, tenantId: String): WebAuthnRegistrationStart {
        val configuration = providerConfiguration.validated()
        val account = activeAccount(userId, tenantId)
        val userHandle = WebAuthnUserHandles.derive(tenantId, userId)
        val repository = TenantWebAuthnCredentialRepository(
            tenantId = tenantId,
            credentialService = credentialService,
            userAccountService = userAccountService,
            knownUsername = account.username,
            knownUserHandle = userHandle,
            knownCredentialIds = credentialService.listActive(tenantId, userId).map { it.credentialId }.toSet(),
        )
        val relyingParty = relyingParty(configuration, repository, requiresDirectAttestation(tenantId))
        val request = relyingParty.startRegistration(
            StartRegistrationOptions.builder()
                .user(
                    UserIdentity.builder()
                        .name(account.username)
                        .displayName(account.username)
                        .id(userHandle)
                        .build()
                )
                .authenticatorSelection(
                    AuthenticatorSelectionCriteria.builder()
                        .residentKey(configuration.residentKey)
                        .userVerification(configuration.userVerification)
                        .build()
                )
                .extensions(RegistrationExtensionInputs.builder().credProps().build())
                .timeout(configuration.browserTimeoutMillis)
                .build()
        )
        val now = clock.instant()
        val expiresAt = now.plusSeconds(configuration.ceremonyTtlSeconds)
        repeat(MAX_ID_GENERATION_ATTEMPTS) {
            val id = randomId()
            if (ceremonyStore.create(
                    WebAuthnCeremonyState(
                        id = id,
                        type = WebAuthnCeremonyTypeEnum.REGISTRATION,
                        tenantId = tenantId,
                        userId = userId,
                        requestJson = request.toJson(),
                        createdAt = now,
                        expiresAt = expiresAt,
                    )
                )
            ) {
                return WebAuthnRegistrationStart(id, request.toCredentialsCreateJson(), expiresAt)
            }
        }
        fail("WEBAUTHN_CEREMONY_ID_EXHAUSTED")
    }

    open fun finish(
        userId: String,
        tenantId: String,
        command: WebAuthnRegistrationFinishCommand,
    ): WebAuthnRegistrationFinish {
        validateFinishInput(command)
        val state = ceremonyStore.consume(command.ceremonyId)
            ?.takeIf {
                it.type == WebAuthnCeremonyTypeEnum.REGISTRATION &&
                    it.tenantId == tenantId && it.userId == userId
            }
            ?: fail("WEBAUTHN_CEREMONY_INVALID")
        val configuration = providerConfiguration.validated()
        val account = activeAccount(userId, tenantId)
        val userHandle = WebAuthnUserHandles.derive(tenantId, userId)
        val request = runCatching { PublicKeyCredentialCreationOptions.fromJson(state.requestJson) }
            .getOrElse { fail("WEBAUTHN_CEREMONY_INVALID", it) }
        if (request.rp.id != configuration.rpId || request.user.id != userHandle) {
            fail("WEBAUTHN_CEREMONY_INVALID")
        }
        val repository = TenantWebAuthnCredentialRepository(
            tenantId = tenantId,
            credentialService = credentialService,
            userAccountService = userAccountService,
            knownUsername = account.username,
            knownUserHandle = userHandle,
            knownCredentialIds = credentialService.listActive(tenantId, userId).map { it.credentialId }.toSet(),
        )
        val verified = runCatching {
            registrationVerifier.verify(
                WebAuthnRegistrationVerificationRequest(
                    request = request,
                    credentialResponseJson = command.credentialResponseJson,
                    rpId = configuration.rpId,
                    rpName = configuration.rpName,
                    origins = configuration.origins,
                    credentialRepository = repository,
                    clock = clock,
                )
            )
        }.getOrElse { fail("WEBAUTHN_REGISTRATION_VERIFICATION_FAILED", it) }
        if (configuration.userVerification == UserVerificationRequirement.REQUIRED && !verified.userVerified) {
            fail("WEBAUTHN_USER_VERIFICATION_REQUIRED")
        }
        try {
            attestationPolicyService?.enforce(
                tenantId = tenantId,
                aaguid = verified.aaguid,
                attestationFormat = verified.attestationFormat,
                attestationTrusted = verified.attestationTrusted,
            )
        } catch (e: WebAuthnAttestationPolicyException) {
            fail(e.errorCode, e)
        }
        if (credentialService.findActive(tenantId, verified.credentialId) != null) {
            fail("WEBAUTHN_CREDENTIAL_ALREADY_REGISTERED")
        }
        val credential = credentialService.registerVerified(
            VerifiedWebAuthnCredentialRegistration(
                tenantId = tenantId,
                userId = userId,
                credentialId = verified.credentialId,
                userHandle = userHandle.base64Url,
                publicKeyCose = verified.publicKeyCose,
                signatureCount = verified.signatureCount,
                transports = verified.transports,
                aaguid = verified.aaguid,
                attestationFormat = verified.attestationFormat,
                backupEligible = verified.backupEligible,
                backedUp = verified.backedUp,
                discoverable = verified.discoverable,
                displayName = command.displayName,
            )
        )
        return WebAuthnRegistrationFinish(
            credential = credential,
            userVerified = verified.userVerified,
            attestationTrusted = verified.attestationTrusted,
        )
    }

    private fun validateFinishInput(command: WebAuthnRegistrationFinishCommand) {
        if (!CEREMONY_ID.matches(command.ceremonyId)) fail("WEBAUTHN_CEREMONY_INVALID")
        if (command.credentialResponseJson.isBlank() || command.credentialResponseJson.length > MAX_RESPONSE_JSON_CHARS) {
            fail("WEBAUTHN_REGISTRATION_RESPONSE_INVALID")
        }
        if (command.displayName.trim().isBlank() || command.displayName.trim().length > 100) {
            fail("WEBAUTHN_DISPLAY_NAME_INVALID")
        }
    }

    private fun activeAccount(userId: String, tenantId: String): UserAccount =
        userAccountService.get(userId)
            ?.takeIf { it.tenantId == tenantId && it.active }
            ?: fail("WEBAUTHN_ACCOUNT_NOT_FOUND")

    private fun requiresDirectAttestation(tenantId: String): Boolean = try {
        attestationPolicyService?.getEffective(tenantId)?.let { policy ->
            policy.requireTrustedAttestation ||
                policy.allowedAttestationFormats.isNotEmpty() ||
                policy.aaguidMode != WebAuthnAaguidPolicyModeEnum.NONE
        } == true
    } catch (e: WebAuthnAttestationPolicyException) {
        fail(e.errorCode, e)
    }

    private fun relyingParty(
        configuration: ValidatedWebAuthnProviderConfiguration,
        repository: CredentialRepository,
        requestDirectAttestation: Boolean,
    ): RelyingParty = RelyingParty.builder()
        .identity(
            RelyingPartyIdentity.builder()
                .id(configuration.rpId)
                .name(configuration.rpName)
                .build()
        )
        .credentialRepository(repository)
        .origins(configuration.origins)
        .attestationConveyancePreference(
            if (requestDirectAttestation) {
                AttestationConveyancePreference.DIRECT
            } else {
                AttestationConveyancePreference.NONE
            }
        )
        .allowUntrustedAttestation(true)
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
