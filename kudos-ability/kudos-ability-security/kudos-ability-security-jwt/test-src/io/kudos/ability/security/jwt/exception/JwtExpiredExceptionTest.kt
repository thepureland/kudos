package io.kudos.ability.security.jwt.exception

import org.springframework.security.oauth2.jwt.BadJwtException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [JwtExpiredException] — both constructors.
 *
 * The class carries no logic of its own; what matters (and what these tests lock down) is the
 * type hierarchy: it must remain a [BadJwtException] so Spring Security's existing
 * exception-translator wiring keeps mapping it to a 401 without apps registering a special
 * handler.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class JwtExpiredExceptionTest {

    @Test
    fun messageOnlyConstructor_preservesMessage_andIsBadJwtException() {
        val ex = JwtExpiredException("exp is invalid, 2026-01-01T00:00:00Z")
        assertEquals("exp is invalid, 2026-01-01T00:00:00Z", ex.message)
        assertTrue(
            ex is BadJwtException,
            "JwtExpiredException must extend BadJwtException so the 401 mapping fires",
        )
    }

    @Test
    fun messageAndCauseConstructor_preservesBoth() {
        val cause = IllegalStateException("clock drifted")
        val ex = JwtExpiredException("token expired", cause)
        assertEquals("token expired", ex.message)
        assertSame(cause, ex.cause, "cause must be propagated unchanged")
    }
}
