package io.kudos.ms.auth.common.authentication.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * Method-neutral action payload.
 *
 * Only the selected method interprets credential fields. None of these values are persisted in
 * [io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction].
 */
data class AuthenticationActionRequest(
    @get:MaxLength(36)
    val tenantId: String? = null,
    @get:MaxLength(32)
    val method: String? = null,
    // Authentication methods may use an RFC-length email address as the login identifier.
    @get:MaxLength(254)
    val username: String? = null,
    val plainPassword: String? = null,
    @get:MaxLength(32)
    val code: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val loginIp: Long? = null,
    val loginDevice: String? = null,
    val loginBrowser: String? = null,
    val loginOs: String? = null,
    val userAgent: String? = null,
)
