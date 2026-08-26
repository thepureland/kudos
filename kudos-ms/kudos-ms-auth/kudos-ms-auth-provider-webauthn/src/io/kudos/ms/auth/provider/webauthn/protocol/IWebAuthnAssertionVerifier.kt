package io.kudos.ms.auth.provider.webauthn.protocol

import com.yubico.webauthn.AssertionRequest
import com.yubico.webauthn.CredentialRepository
import java.time.Clock

data class WebAuthnAssertionVerificationRequest(
    val request: AssertionRequest,
    val credentialResponseJson: String,
    val rpId: String,
    val rpName: String,
    val origins: Set<String>,
    val credentialRepository: CredentialRepository,
    val clock: Clock,
)

data class VerifiedWebAuthnAssertionResult(
    val credentialId: String,
    val userHandle: String,
    val username: String,
    val signatureCount: Long,
    val userVerified: Boolean,
    val backupEligible: Boolean,
    val backedUp: Boolean,
)

interface IWebAuthnAssertionVerifier {
    fun verify(request: WebAuthnAssertionVerificationRequest): VerifiedWebAuthnAssertionResult
}
