package io.kudos.ms.auth.token.jwt

import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.token.jwt.model.AccessTokenException
import io.kudos.ms.auth.token.jwt.model.IJwtAccessTokenService
import io.kudos.ms.auth.token.jwt.model.IssuedAccessToken
import io.kudos.ms.auth.token.jwt.web.JwtBearerAuthenticationFilter
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class JwtBearerAuthenticationFilterTest {

    @AfterTest
    fun clearContext() = KudosContextHolder.clear()

    @Test
    fun validBearer_populatesTrustedCurrentUser() {
        val filter = JwtBearerAuthenticationFilter(StubAccessTokenService(valid = true))
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer valid-token")
        }

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals("u-1", KudosContextHolder.get().user?.id)
        assertEquals("t-1", (KudosContextHolder.get().user as SessionUserPrincipal).tenantId)
        assertEquals(
            "s-1",
            (request.getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE) as AuthenticationSession).id,
        )
    }

    @Test
    fun invalidBearer_returns401WithoutPopulatingContext() {
        val filter = JwtBearerAuthenticationFilter(StubAccessTokenService(valid = false))
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer invalid-token")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertEquals(401, response.status)
        assertNull(request.getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE))
        assertNull(KudosContextHolder.getOrNull()?.user)
    }

    private class StubAccessTokenService(private val valid: Boolean) : IJwtAccessTokenService {
        override fun issue(session: AuthenticationSession): IssuedAccessToken = error("not used")

        override fun verify(token: String): AuthenticationSession {
            if (!valid) throw AccessTokenException()
            val now = Instant.now()
            return AuthenticationSession(
                id = "s-1",
                tenantId = "t-1",
                userId = "u-1",
                username = "alice",
                authTime = now,
                amr = setOf("password"),
                acr = "urn:kudos:acr:password",
                createdAt = now,
                lastSeenAt = now,
                idleExpiresAt = now.plusSeconds(60),
                absoluteExpiresAt = now.plusSeconds(120),
            )
        }
    }
}
