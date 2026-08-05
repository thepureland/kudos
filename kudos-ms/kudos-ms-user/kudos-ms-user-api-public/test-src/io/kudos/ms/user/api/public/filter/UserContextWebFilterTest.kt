package io.kudos.ms.user.api.public.filter

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure unit test for [UserContextWebFilter].
 *
 * No Spring context, no DB. Uses Spring's [MockHttpServletRequest] / [MockHttpSession] and a
 * recording [FilterChain]. Drives [KudosContextHolder] directly to assert the one-way
 * "read session -> write context.user" behavior.
 *
 * Covers:
 *  - no session at all -> context.user untouched, chain still invoked
 *  - session exists but no principal attribute -> context.user untouched
 *  - session holds a non-[SessionUserPrincipal] attribute -> `is` check fails, context.user untouched
 *  - session holds a [SessionUserPrincipal] -> written into KudosContextHolder.get().user
 *  - filter chain is always continued
 *
 * @author K
 * @since 1.0.0
 */
internal class UserContextWebFilterTest {

    private val filter = UserContextWebFilter()

    @BeforeTest
    fun clearBefore() = KudosContextHolder.clear()

    @AfterTest
    fun clearAfter() = KudosContextHolder.clear()

    /** Records whether the chain was continued. */
    private class RecordingChain : FilterChain {
        var invoked = false
            private set

        override fun doFilter(request: ServletRequest?, response: ServletResponse?) {
            invoked = true
        }
    }

    @Test
    fun noSession_doesNotTouchContextUser_butContinuesChain() {
        val request = MockHttpServletRequest() // getSession(false) == null
        val chain = RecordingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.invoked)
        // filter never called KudosContextHolder.get(), so no context was auto-created
        assertNull(KudosContextHolder.getOrNull())
    }

    @Test
    fun sessionWithoutPrincipalAttribute_doesNotTouchContextUser() {
        val request = MockHttpServletRequest()
        request.setSession(MockHttpSession()) // empty session, no SESSION_KEY_USER
        val chain = RecordingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.invoked)
        assertNull(KudosContextHolder.getOrNull())
    }

    @Test
    fun sessionWithNonPrincipalAttribute_doesNotTouchContextUser() {
        val request = MockHttpServletRequest()
        val session = MockHttpSession()
        session.setAttribute(KudosContext.SESSION_KEY_USER, "not-a-principal")
        request.setSession(session)
        val chain = RecordingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.invoked)
        // `is SessionUserPrincipal` is false -> branch skipped, no context created
        assertNull(KudosContextHolder.getOrNull())
    }

    @Test
    fun sessionWithPrincipal_writesIntoContextUser() {
        val principal = SessionUserPrincipal(id = "u-1", tenantId = "t-1", username = "alice")
        val request = MockHttpServletRequest()
        val session = MockHttpSession()
        session.setAttribute(KudosContext.SESSION_KEY_USER, principal)
        request.setSession(session)
        val chain = RecordingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertTrue(chain.invoked)
        val ctx = KudosContextHolder.getOrNull()
        assertSame(principal, ctx?.user)
    }

    @Test
    fun sessionWithPrincipal_fillsExistingContextWithoutReplacingIt() {
        // a context already bound (e.g. by WebContextInitFilter); the filter only fills user
        val existing = KudosContext()
        KudosContextHolder.set(existing)
        val principal = SessionUserPrincipal(id = "u-9", tenantId = "t-9", username = "carol")
        val request = MockHttpServletRequest()
        val session = MockHttpSession()
        session.setAttribute(KudosContext.SESSION_KEY_USER, principal)
        request.setSession(session)
        val chain = RecordingChain()

        filter.doFilter(request, MockHttpServletResponse(), chain)

        // same context instance reused, only user filled in
        assertSame(existing, KudosContextHolder.getOrNull())
        assertEquals(principal, existing.user)
    }
}
