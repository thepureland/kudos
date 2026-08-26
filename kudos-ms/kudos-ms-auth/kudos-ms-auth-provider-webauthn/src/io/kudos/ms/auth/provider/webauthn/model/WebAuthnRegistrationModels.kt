package io.kudos.ms.auth.provider.webauthn.model

import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialSummary
import java.time.Instant

data class WebAuthnRegistrationStart(
    val ceremonyId: String,
    /** JSON suitable for navigator.credentials.create() after Base64url fields become Uint8Array. */
    val publicKeyCredentialCreationOptions: String,
    val expiresAt: Instant,
)

data class WebAuthnRegistrationFinishCommand(
    val ceremonyId: String,
    /** JSON returned by PublicKeyCredential.toJSON() or an equivalent Base64url JSON adapter. */
    val credentialResponseJson: String,
    val displayName: String,
)

/** Public request body; ceremony, user and tenant are supplied exclusively by trusted server context. */
data class WebAuthnRegistrationFinishRequest(
    val credentialResponseJson: String,
    val displayName: String,
)

data class WebAuthnRegistrationFinish(
    val credential: WebAuthnCredentialSummary,
    val userVerified: Boolean,
    val attestationTrusted: Boolean,
)

class WebAuthnProviderException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
