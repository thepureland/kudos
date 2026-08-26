package io.kudos.ms.auth.common.authentication.mfa.webauthn.attestation.vo

data class WebAuthnAttestationPolicyAdminSaveRequest(
    val aaguidMode: String = "NONE",
    val aaguids: Set<String> = emptySet(),
    val allowedAttestationFormats: Set<String> = emptySet(),
    val requireTrustedAttestation: Boolean = false,
    val reason: String,
)
