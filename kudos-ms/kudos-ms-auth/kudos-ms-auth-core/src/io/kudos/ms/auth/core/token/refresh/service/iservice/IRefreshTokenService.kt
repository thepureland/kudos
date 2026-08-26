package io.kudos.ms.auth.core.token.refresh.service.iservice

import io.kudos.ms.auth.core.token.refresh.model.IssuedRefreshToken

interface IRefreshTokenService {
    fun issue(sourceSessionId: String, clientId: String? = null, deviceId: String? = null): IssuedRefreshToken
    fun rotate(token: String): IssuedRefreshToken
    fun revoke(token: String, reason: String): Boolean
    fun revokeBySession(sessionId: String, reason: String): Int
}
