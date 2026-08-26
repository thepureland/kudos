package io.kudos.ms.auth.core.token.refresh.model

import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import java.time.Instant

/** Raw refresh token is returned only from issue/rotate and must never be logged or persisted. */
data class IssuedRefreshToken(
    val token: String,
    val session: AuthenticationSession,
    val familyId: String,
    val expiresAt: Instant,
)

class RefreshTokenException(
    val errorCode: String,
    val reuseDetected: Boolean = false,
) : IllegalStateException(errorCode)
