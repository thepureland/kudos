package io.kudos.ms.auth.common.provider.vo.response

/** Effective JIT defaults returned without Provider secrets. */
data class IdentityProviderJitConfigAdminResponse(
    val providerId: String,
    val usernameStrategy: String,
    val requireVerifiedEmail: Boolean,
    val allowedEmailDomains: List<String>,
    val defaultOrgId: String?,
    val defaultSupervisorId: String?,
    val accountTypeDictCode: String?,
    val accountStatusDictCode: String?,
    val defaultLocale: String?,
    val defaultTimezone: String?,
    val defaultCurrency: String?,
    val configured: Boolean,
)
