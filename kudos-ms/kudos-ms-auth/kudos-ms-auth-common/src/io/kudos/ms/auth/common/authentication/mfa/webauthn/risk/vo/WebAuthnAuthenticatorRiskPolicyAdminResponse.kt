package io.kudos.ms.auth.common.authentication.mfa.webauthn.risk.vo

import java.time.LocalDateTime

data class WebAuthnAuthenticatorRiskPolicyAdminResponse(
    val blockedRiskLevels: Set<String>,
    val riskEvaluationAvailable: Boolean,
    val effectiveFrom: LocalDateTime?,
    val configured: Boolean,
)
