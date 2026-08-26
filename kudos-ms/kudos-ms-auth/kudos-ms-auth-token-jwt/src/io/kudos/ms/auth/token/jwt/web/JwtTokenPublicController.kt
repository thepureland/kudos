package io.kudos.ms.auth.token.jwt.web

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.core.token.refresh.model.IssuedRefreshToken
import io.kudos.ms.auth.core.token.refresh.model.RefreshTokenException
import io.kudos.ms.auth.core.token.refresh.service.iservice.IRefreshTokenService
import io.kudos.ms.auth.token.jwt.model.IJwtAccessTokenService
import io.kudos.ms.auth.token.jwt.model.TokenIssueRequest
import io.kudos.ms.auth.token.jwt.model.TokenPairResponse
import io.kudos.ms.auth.token.jwt.model.TokenRefreshRequest
import io.kudos.ms.auth.token.jwt.model.TokenRevokeRequest
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant

/** Session-to-token exchange plus refresh rotation; never accepts a user id from the caller. */
@RestController
@RequestMapping("/api/public/auth/token")
open class JwtTokenPublicController(
    private val accessTokenService: IJwtAccessTokenService,
    private val refreshTokenService: IRefreshTokenService,
    private val sessionService: IAuthenticationSessionService,
) {

    /** Exchanges an already authenticated browser/client session for an independent API session. */
    @PostMapping
    open fun issue(
        @RequestBody @Valid request: TokenIssueRequest,
        servletRequest: HttpServletRequest,
    ): TokenPairResponse {
        val httpSession = servletRequest.getSession(false) ?: unauthorized()
        val principal = httpSession.getAttribute(KudosContext.SESSION_KEY_USER) as? SessionUserPrincipal
            ?: unauthorized()
        val sourceId = httpSession.getAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE) as? String
            ?: unauthorized()
        val source = sessionService.get(sourceId)
            ?.takeIf {
                it.isActive() && it.userId == principal.id && it.tenantId == principal.tenantId
            }
            ?: unauthorized()
        return pair(refreshTokenService.issue(source.id, request.clientId, request.deviceId))
    }

    @PostMapping("/refresh")
    open fun refresh(@RequestBody @Valid request: TokenRefreshRequest): TokenPairResponse =
        try {
            pair(refreshTokenService.rotate(request.refreshToken))
        } catch (_: RefreshTokenException) {
            unauthorized()
        }

    /** Idempotent and non-enumerating: unknown/repeated tokens receive the same successful response. */
    @PostMapping("/revoke")
    open fun revoke(@RequestBody @Valid request: TokenRevokeRequest): Boolean {
        runCatching { refreshTokenService.revoke(request.refreshToken, USER_TOKEN_REVOKE) }
        return true
    }

    private fun pair(refresh: IssuedRefreshToken): TokenPairResponse {
        val access = try {
            accessTokenService.issue(refresh.session)
        } catch (e: Exception) {
            // DB rotation may already be committed; do not leave an undisclosed live family behind.
            runCatching { refreshTokenService.revoke(refresh.token, ACCESS_TOKEN_ISSUE_FAILED) }
            throw e
        }
        val now = Instant.now()
        return TokenPairResponse(
            accessToken = access.token,
            expiresIn = Duration.between(now, access.expiresAt).seconds.coerceAtLeast(0),
            refreshToken = refresh.token,
            refreshExpiresIn = Duration.between(now, refresh.expiresAt).seconds.coerceAtLeast(0),
            sessionId = refresh.session.id,
        )
    }

    private fun unauthorized(): Nothing =
        throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN")

    private companion object {
        const val USER_TOKEN_REVOKE = "USER_TOKEN_REVOKE"
        const val ACCESS_TOKEN_ISSUE_FAILED = "ACCESS_TOKEN_ISSUE_FAILED"
    }
}
