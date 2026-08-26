package io.kudos.ms.auth.common.authentication.mfa.policy.vo

data class TenantMfaPolicyAdminSaveRequest(
    val mode: String,
    val gracePeriodDays: Int = 7,
    val allowedMethods: Set<String> = setOf("TOTP"),
    val recoveryCodesEnabled: Boolean = true,
    val requiredAccountTypeCodes: Set<String> = emptySet(),
    val requiredRoleCodes: Set<String> = emptySet(),
    val reason: String,
)
