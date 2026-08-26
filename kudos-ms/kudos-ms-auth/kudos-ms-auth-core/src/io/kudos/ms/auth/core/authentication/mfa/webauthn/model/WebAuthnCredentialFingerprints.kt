package io.kudos.ms.auth.core.authentication.mfa.webauthn.model

import java.security.MessageDigest
import java.util.Base64

/** Produces a stable, non-reversible audit identifier without exposing the raw credential id. */
object WebAuthnCredentialFingerprints {
    fun sha256(credentialId: String): String {
        val decoded = Base64.getUrlDecoder().decode(credentialId)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(decoded)
        )
    }
}
