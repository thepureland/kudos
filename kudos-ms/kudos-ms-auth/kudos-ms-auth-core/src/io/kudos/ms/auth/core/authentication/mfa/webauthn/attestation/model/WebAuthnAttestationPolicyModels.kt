package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model

import java.time.LocalDateTime

enum class WebAuthnAaguidPolicyModeEnum {
    NONE,
    ALLOW_LIST,
    DENY_LIST,
}

data class WebAuthnAttestationPolicySaveCommand(
    val tenantId: String,
    val aaguidMode: String,
    val aaguids: Set<String>,
    val allowedAttestationFormats: Set<String>,
    val requireTrustedAttestation: Boolean,
    val actorUserId: String,
    val operationReason: String,
)

data class EffectiveWebAuthnAttestationPolicy(
    val tenantId: String,
    val aaguidMode: WebAuthnAaguidPolicyModeEnum = WebAuthnAaguidPolicyModeEnum.NONE,
    val aaguids: Set<String> = emptySet(),
    val allowedAttestationFormats: Set<String> = emptySet(),
    val requireTrustedAttestation: Boolean = false,
    val effectiveFrom: LocalDateTime? = null,
    val configured: Boolean = false,
)

class WebAuthnAttestationPolicyException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
