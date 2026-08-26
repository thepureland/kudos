package io.kudos.ms.auth.common.authentication.vo.request

/** Administrator request for revoking one logical authentication session. */
data class AuthenticationSessionAdminRevokeRequest(
    val userId: String,
    val reason: String,
)
