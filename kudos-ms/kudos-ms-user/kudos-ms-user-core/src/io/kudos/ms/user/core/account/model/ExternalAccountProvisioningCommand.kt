package io.kudos.ms.user.core.account.model

/** Verified external identity and local defaults used for JIT account provisioning. */
data class ExternalAccountProvisioningCommand(
    val username: String,
    val tenantId: String,
    val identityProviderId: String,
    val providerCode: String,
    val issuer: String?,
    val subject: String,
    val unionId: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val defaultLocale: String? = null,
    val defaultTimezone: String? = null,
    val defaultCurrency: String? = null,
    val defaultOrgId: String? = null,
    val defaultSupervisorId: String? = null,
    val accountTypeDictCode: String? = null,
    val accountStatusDictCode: String? = null,
)
