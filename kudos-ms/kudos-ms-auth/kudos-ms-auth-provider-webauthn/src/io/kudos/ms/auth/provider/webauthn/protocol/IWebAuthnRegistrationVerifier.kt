package io.kudos.ms.auth.provider.webauthn.protocol

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import java.time.Clock

data class WebAuthnRegistrationVerificationRequest(
    val request: PublicKeyCredentialCreationOptions,
    val credentialResponseJson: String,
    val rpId: String,
    val rpName: String,
    val origins: Set<String>,
    val credentialRepository: CredentialRepository,
    val clock: Clock,
)

data class VerifiedWebAuthnRegistrationResult(
    val credentialId: String,
    val publicKeyCose: String,
    val signatureCount: Long,
    val transports: Set<String>,
    val aaguid: String,
    val attestationFormat: String,
    val backupEligible: Boolean,
    val backedUp: Boolean,
    val discoverable: Boolean,
    val userVerified: Boolean,
    val attestationTrusted: Boolean,
)

interface IWebAuthnRegistrationVerifier {
    fun verify(request: WebAuthnRegistrationVerificationRequest): VerifiedWebAuthnRegistrationResult
}
