package io.kudos.ms.auth.common.authentication.mfa.policy.vo

data class TenantMfaPolicyAdminResponse(
    val mode: String,
    val gracePeriodDays: Int,
    val allowedMethods: Set<String>,
    val recoveryCodesEnabled: Boolean,
    val requiredAccountTypeCodes: Set<String>,
    val requiredRoleCodes: Set<String>,
    val configured: Boolean,
)
