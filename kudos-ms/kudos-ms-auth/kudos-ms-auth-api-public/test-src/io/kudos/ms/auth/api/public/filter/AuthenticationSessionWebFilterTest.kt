package io.kudos.ms.auth.api.public.filter

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AuthenticationSessionWebFilterTest {

    private val service = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
    private val filter = AuthenticationSessionWebFilter(service)

    @AfterTest
    fun clearContext() = KudosContextHolder.clear()

    private class RecordingChain : FilterChain {
        var invoked = false
        override fun doFilter(request: ServletRequest?, response: ServletResponse?) {
            invoked = true
        }
    }

    private fun issuedSession() = service.issue(
        AuthenticationSessionIssueCommand(
            AuthenticationContext(
                userId = "u-1",
                tenantId = "t-1",
                authTime = Instant.now(),
                amr = setOf("password"),
                acr = "urn:kudos:acr:password",
            ),
            username = "alice",
        )
    )

    private fun request(id: String, principal: SessionUserPrincipal): Pair<MockHttpServletRequest, MockHttpSession> {
        val session = MockHttpSession().apply {
            setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, id)
            setAttribute(KudosContext.SESSION_KEY_USER, principal)
        }
        return MockHttpServletRequest().apply { setSession(session) } to session
    }

    @Test
    fun validSession_touchesRegistryAndPopulatesContext() {
        val issued = issuedSession()
        val (request, _) = request(issued.id, SessionUserPrincipal("u-1", "t-1", "alice"))
        val chain = RecordingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.invoked)
        assertEquals("u-1", KudosContextHolder.getOrNull()?.user?.id)
        assertEquals(issued.id, request.getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE)?.let {
            (it as AuthenticationSession).id
        })
        assertEquals(issued.version + 1, service.get(issued.id)?.version)
    }

    @Test
    fun revokedSession_isInvalidatedAndDoesNotPopulateContext() {
        val issued = issuedSession()
        service.revokeForUser(issued.id, "t-1", "u-1", "ADMIN_REVOKE")
        val (request, httpSession) = request(
            issued.id,
            SessionUserPrincipal("u-1", "t-1", "alice"),
        )
        val chain = RecordingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.invoked)
        assertTrue(httpSession.isInvalid)
        assertNull(request.getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE))
        assertNull(KudosContextHolder.getOrNull())
    }

    @Test
    fun legacySessionWithoutLogicalId_isLeftForCompatibilityFilter() {
        val session = MockHttpSession().apply {
            setAttribute(KudosContext.SESSION_KEY_USER, SessionUserPrincipal("u-1", "t-1", "alice"))
        }
        val request = MockHttpServletRequest().apply { setSession(session) }
        val chain = RecordingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.invoked)
        assertTrue(!session.isInvalid)
        assertNull(KudosContextHolder.getOrNull())
    }
}
