package io.kudos.ms.auth.provider.webauthn.model

import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialSummary
import java.time.Instant

data class WebAuthnAssertionStart(
    val ceremonyId: String,
    /** JSON suitable for navigator.credentials.get() after Base64url fields become Uint8Array. */
    val publicKeyCredentialRequestOptions: String,
    val expiresAt: Instant,
)

data class WebAuthnAssertionFinishCommand(
    val ceremonyId: String,
    /** JSON returned by PublicKeyCredential.toJSON() or an equivalent Base64url JSON adapter. */
    val credentialResponseJson: String,
)

data class WebAuthnAssertionFinish(
    val userId: String,
    val username: String,
    val credential: WebAuthnCredentialSummary,
    val userVerified: Boolean,
    val backupEligible: Boolean,
    val backedUp: Boolean,
)
