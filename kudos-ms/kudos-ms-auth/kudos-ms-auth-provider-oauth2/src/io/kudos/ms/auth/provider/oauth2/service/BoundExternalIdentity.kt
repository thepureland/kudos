package io.kudos.ms.auth.provider.oauth2.service

import io.kudos.ms.auth.common.provider.vo.ExternalPrincipal

data class BoundExternalIdentity(
    val userId: String,
    val tenantId: String,
    val username: String,
    val providerCode: String,
    val principal: ExternalPrincipal,
)

class ExternalIdentityAuthenticationException(
    val errorCode: String,
    cause: Throwable? = null,
) : RuntimeException(errorCode, cause)
