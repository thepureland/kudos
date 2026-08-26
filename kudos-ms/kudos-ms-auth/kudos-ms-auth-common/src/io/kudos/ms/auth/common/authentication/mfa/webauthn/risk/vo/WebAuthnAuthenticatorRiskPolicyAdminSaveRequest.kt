package io.kudos.ms.auth.common.authentication.mfa.webauthn.risk.vo

data class WebAuthnAuthenticatorRiskPolicyAdminSaveRequest(
    val blockedRiskLevels: Set<String> = emptySet(),
    val reason: String,
)
