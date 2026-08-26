package io.kudos.ms.auth.common.provider.vo.request

/** Tenant-admin command for the JIT defaults of one Provider instance. */
data class IdentityProviderJitConfigAdminSaveRequest(
    val providerId: String,
    val usernameStrategy: String,
    val requireVerifiedEmail: Boolean = false,
    val allowedEmailDomains: List<String> = emptyList(),
    val defaultOrgId: String? = null,
    val defaultSupervisorId: String? = null,
    val accountTypeDictCode: String? = null,
    val accountStatusDictCode: String? = null,
    val defaultLocale: String? = null,
    val defaultTimezone: String? = null,
    val defaultCurrency: String? = null,
    val reason: String,
)
