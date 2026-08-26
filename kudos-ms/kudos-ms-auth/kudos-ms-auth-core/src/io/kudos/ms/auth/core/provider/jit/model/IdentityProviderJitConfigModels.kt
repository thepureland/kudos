package io.kudos.ms.auth.core.provider.jit.model

import io.kudos.ms.auth.common.provider.enums.ExternalJitUsernameStrategyEnum

data class IdentityProviderJitConfigSaveCommand(
    val providerId: String,
    val tenantId: String,
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
    val actorUserId: String,
    val operationReason: String,
)

data class EffectiveIdentityProviderJitConfig(
    val providerId: String,
    val usernameStrategy: ExternalJitUsernameStrategyEnum =
        ExternalJitUsernameStrategyEnum.EXTERNAL_USERNAME_HASHED,
    val requireVerifiedEmail: Boolean = false,
    val allowedEmailDomains: List<String> = emptyList(),
    val defaultOrgId: String? = null,
    val defaultSupervisorId: String? = null,
    val accountTypeDictCode: String? = null,
    val accountStatusDictCode: String? = null,
    val defaultLocale: String? = null,
    val defaultTimezone: String? = null,
    val defaultCurrency: String? = null,
    val configured: Boolean = false,
)

class IdentityProviderJitConfigException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
