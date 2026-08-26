package io.kudos.ms.auth.token.jwt.model

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import java.time.Instant

data class IssuedAccessToken(
    val token: String,
    val expiresAt: Instant,
)

data class TokenIssueRequest(
    @get:MaxLength(128)
    val clientId: String? = null,
    @get:MaxLength(128)
    val deviceId: String? = null,
)

data class TokenRefreshRequest(
    @get:MaxLength(128)
    val refreshToken: String,
)

data class TokenRevokeRequest(
    @get:MaxLength(128)
    val refreshToken: String,
)

data class TokenPairResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshToken: String,
    val refreshExpiresIn: Long,
    val sessionId: String,
)

class AccessTokenException : IllegalStateException("INVALID_ACCESS_TOKEN")

interface IJwtAccessTokenService {
    fun issue(session: AuthenticationSession): IssuedAccessToken
    fun verify(token: String): AuthenticationSession
}
