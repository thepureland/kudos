package io.kudos.ms.auth.token.jwt

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.auth.core.token.refresh.model.IssuedRefreshToken
import io.kudos.ms.auth.core.token.refresh.model.RefreshTokenException
import io.kudos.ms.auth.core.token.refresh.service.iservice.IRefreshTokenService
import io.kudos.ms.auth.token.jwt.model.IJwtAccessTokenService
import io.kudos.ms.auth.token.jwt.model.IssuedAccessToken
import io.kudos.ms.auth.token.jwt.model.TokenIssueRequest
import io.kudos.ms.auth.token.jwt.model.TokenRefreshRequest
import io.kudos.ms.auth.token.jwt.model.TokenRevokeRequest
import io.kudos.ms.auth.token.jwt.web.JwtTokenPublicController
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class JwtTokenPublicControllerTest {

    private val sessionService = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
    private val refreshService = StubRefreshTokenService()
    private val accessService = StubAccessTokenService()
    private val controller = JwtTokenPublicController(
        accessService,
        refreshService,
        sessionService,
    )

    private fun source() = sessionService.issue(
        AuthenticationSessionIssueCommand(
            context = AuthenticationContext(
                userId = "u-1",
                tenantId = "t-1",
                authTime = Instant.now(),
                amr = setOf("password"),
                acr = "urn:kudos:acr:password",
            ),
            username = "alice",
        )
    )

    private fun request(session: AuthenticationSession) = MockHttpServletRequest().apply {
        getSession(true)!!.setAttribute(
            KudosContext.SESSION_KEY_USER,
            SessionUserPrincipal(session.userId, session.tenantId, session.username.orEmpty()),
        )
        getSession(false)!!.setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, session.id)
    }

    @Test
    fun issue_usesOnlyAuthenticatedLogicalSessionAsSubject() {
        val source = source()
        refreshService.session = source

        val result = controller.issue(TokenIssueRequest("mobile", "phone"), request(source))

        assertEquals(source.id, refreshService.issuedFromSessionId)
        assertEquals("mobile", refreshService.clientId)
        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
    }

    @Test
    fun issue_withoutAuthenticatedSessionIsRejected() {
        assertFailsWith<ResponseStatusException> {
            controller.issue(TokenIssueRequest(), MockHttpServletRequest())
        }
    }

    @Test
    fun refresh_collapsesServiceFailureToUnauthorized() {
        refreshService.failRotation = true

        val error = assertFailsWith<ResponseStatusException> {
            controller.refresh(TokenRefreshRequest("krt_invalid"))
        }

        assertEquals(401, error.statusCode.value())
    }

    @Test
    fun revoke_isIdempotentAndNonEnumerating() {
        refreshService.revokeResult = false

        assertTrue(controller.revoke(TokenRevokeRequest("krt_unknown")))
    }

    @Test
    fun accessEncodingFailure_revokesNewRefreshFamily() {
        val source = source()
        refreshService.session = source
        accessService.failIssue = true

        assertFailsWith<IllegalStateException> {
            controller.issue(TokenIssueRequest(), request(source))
        }
        assertEquals("refresh-token", refreshService.revokedToken)
        assertEquals("ACCESS_TOKEN_ISSUE_FAILED", refreshService.revokeReason)
    }

    private class StubAccessTokenService : IJwtAccessTokenService {
        var failIssue = false

        override fun issue(session: AuthenticationSession) =
            if (failIssue) error("encode failed")
            else IssuedAccessToken("access-token", Instant.now().plusSeconds(300))

        override fun verify(token: String): AuthenticationSession = error("not used")
    }

    private class StubRefreshTokenService : IRefreshTokenService {
        lateinit var session: AuthenticationSession
        var issuedFromSessionId: String? = null
        var clientId: String? = null
        var failRotation = false
        var revokeResult = true
        var revokedToken: String? = null
        var revokeReason: String? = null

        override fun issue(sourceSessionId: String, clientId: String?, deviceId: String?): IssuedRefreshToken {
            issuedFromSessionId = sourceSessionId
            this.clientId = clientId
            return issued()
        }

        override fun rotate(token: String): IssuedRefreshToken {
            if (failRotation) throw RefreshTokenException("INVALID_REFRESH_TOKEN")
            return issued()
        }

        override fun revoke(token: String, reason: String): Boolean {
            revokedToken = token
            revokeReason = reason
            return revokeResult
        }
        override fun revokeBySession(sessionId: String, reason: String): Int = 0

        private fun issued() = IssuedRefreshToken(
            token = "refresh-token",
            session = session,
            familyId = "family-1",
            expiresAt = Instant.now().plusSeconds(600),
        )
    }
}
