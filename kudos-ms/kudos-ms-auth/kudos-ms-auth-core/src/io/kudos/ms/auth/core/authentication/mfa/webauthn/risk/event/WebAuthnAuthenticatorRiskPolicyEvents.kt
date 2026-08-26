package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.event

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import java.time.Instant

/**
 * Emitted synchronously when an otherwise valid WebAuthn assertion is blocked by tenant policy.
 * The raw credential id, AAGUID and assertion payload are intentionally absent.
 */
data class WebAuthnAuthenticatorRiskPolicyBlocked(
    val tenantId: String,
    val userId: String,
    val credentialIdFingerprint: String,
    val riskLevel: WebAuthnAuthenticatorRiskLevelEnum,
    val riskSources: Set<String>,
    val riskStatusCodes: Set<String>,
    val occurredAt: Instant,
)
