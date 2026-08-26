package io.kudos.ms.auth.common.authentication.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/** Starts an authentication transaction without carrying any credential. */
data class AuthenticationTransactionCreateRequest(
    @get:MaxLength(36)
    val tenantId: String? = null,
    @get:MaxLength(32)
    val requestedMethod: String? = null,
)
