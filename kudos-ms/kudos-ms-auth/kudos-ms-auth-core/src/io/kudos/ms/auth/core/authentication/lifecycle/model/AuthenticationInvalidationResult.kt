package io.kudos.ms.auth.core.authentication.lifecycle.model

/** Result of invalidating every reusable authentication artifact owned by one tenant user. */
data class AuthenticationInvalidationResult(
    val tokenEpoch: Long,
    val revokedSessionCount: Int,
    val revokedRefreshTokenCount: Int,
)
