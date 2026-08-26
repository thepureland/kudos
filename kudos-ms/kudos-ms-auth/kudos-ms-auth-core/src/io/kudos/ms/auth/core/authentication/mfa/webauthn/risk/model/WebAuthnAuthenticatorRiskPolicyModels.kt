package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskSummary
import java.time.LocalDateTime

data class WebAuthnAuthenticatorRiskPolicySaveCommand(
    val tenantId: String,
    val blockedRiskLevels: Set<String>,
    val actorUserId: String,
    val operationReason: String,
)

data class EffectiveWebAuthnAuthenticatorRiskPolicy(
    val tenantId: String,
    val blockedRiskLevels: Set<WebAuthnAuthenticatorRiskLevelEnum> = emptySet(),
    val effectiveFrom: LocalDateTime? = null,
    val configured: Boolean = false,
)

data class WebAuthnAuthenticatorRiskEnforcementCommand(
    val tenantId: String,
    val userId: String,
    val credentialIdFingerprint: String,
    val risk: WebAuthnAuthenticatorRiskSummary,
)

class WebAuthnAuthenticatorRiskPolicyException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
