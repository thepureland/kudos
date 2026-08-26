package io.kudos.ms.auth.provider.oauth2.service

import io.kudos.ms.auth.common.provider.vo.ExternalPrincipal

/** Provider-catalog metadata paired with a principal already verified by Spring Security. */
data class ResolvedExternalPrincipal(
    val tenantId: String,
    val providerId: String,
    val providerCode: String,
    val principal: ExternalPrincipal,
)
