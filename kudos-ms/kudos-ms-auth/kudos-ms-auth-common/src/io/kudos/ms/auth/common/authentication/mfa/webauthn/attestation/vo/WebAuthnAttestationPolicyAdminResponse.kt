package io.kudos.ms.auth.common.authentication.mfa.webauthn.attestation.vo

import java.time.LocalDateTime

data class WebAuthnAttestationPolicyAdminResponse(
    val aaguidMode: String,
    val aaguids: Set<String>,
    val allowedAttestationFormats: Set<String>,
    val requireTrustedAttestation: Boolean,
    val trustSourceAvailable: Boolean,
    val effectiveFrom: LocalDateTime?,
    val configured: Boolean,
)
