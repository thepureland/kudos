package io.kudos.ms.auth.api.public.controller.authentication

import io.kudos.context.core.KudosContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AuthenticationSessionPublicControllerTest {

    private val service = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
    private val controller = AuthenticationSessionPublicController(service)

    private fun issue() = service.issue(
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

    private fun request(authSession: AuthenticationSession, principalUserId: String = "u-1") =
        MockHttpServletRequest().apply {
            getSession(true)!!.setAttribute(
                KudosContext.SESSION_KEY_USER,
                SessionUserPrincipal(principalUserId, "t-1", "alice"),
            )
            getSession(false)!!.setAttribute(AuthenticationSession.HTTP_SESSION_ATTRIBUTE, authSession.id)
        }

    @Test
    fun getCurrent_returnsOnlyTheMatchingLogicalSession() {
        val issued = issue()
        val result = assertNotNull(controller.getCurrent(request(issued)))

        assertEquals(issued.id, result.id)
        assertEquals(issued.version, result.version)
    }

    @Test
    fun getCurrent_mismatchFailsClosedAndInvalidatesHttpSession() {
        val issued = issue()
        val request = request(issued, principalUserId = "u-other")
        val httpSession = request.session as MockHttpSession

        assertNull(controller.getCurrent(request))
        assertTrue(httpSession.isInvalid)
    }

    @Test
    fun revokeCurrent_revokesRegistryAndInvalidatesHttpSession() {
        val issued = issue()
        val request = request(issued)
        val httpSession = request.session as MockHttpSession

        assertTrue(controller.revokeCurrent(request))

        assertTrue(httpSession.isInvalid)
        val revoked = assertNotNull(service.get(issued.id))
        assertFalse(revoked.isActive())
        assertEquals("USER_LOGOUT", revoked.revokeReason)
    }

    @Test
    fun revokeCurrent_withoutSession_returnsFalse() {
        assertFalse(controller.revokeCurrent(MockHttpServletRequest()))
    }

    @Test
    fun list_returnsAllActiveSessionsForAuthenticatedOwner() {
        val current = issue()
        val other = issue()

        val sessions = controller.list(request(current))

        assertEquals(setOf(current.id, other.id), sessions.map(AuthenticationSession::id).toSet())
    }

    @Test
    fun revoke_ownedRemoteSession_keepsCurrentHttpSessionActive() {
        val current = issue()
        val remote = issue()
        val request = request(current)
        val httpSession = request.session as MockHttpSession

        assertTrue(controller.revoke(remote.id, request))

        assertFalse(httpSession.isInvalid)
        assertFalse(assertNotNull(service.get(remote.id)).isActive())
        assertEquals(listOf(current.id), controller.list(request).map(AuthenticationSession::id))
    }

    @Test
    fun revoke_unownedSession_isRejected() {
        val current = issue()
        val otherOwner = service.issue(
            AuthenticationSessionIssueCommand(
                context = AuthenticationContext(
                    userId = "u-other",
                    tenantId = "t-1",
                    authTime = Instant.now(),
                    amr = setOf("password"),
                    acr = "urn:kudos:acr:password",
                ),
            )
        )

        assertFalse(controller.revoke(otherOwner.id, request(current)))
        assertTrue(assertNotNull(service.get(otherOwner.id)).isActive())
    }
}
