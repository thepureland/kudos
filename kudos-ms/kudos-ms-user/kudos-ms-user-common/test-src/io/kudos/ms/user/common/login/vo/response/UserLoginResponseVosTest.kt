package io.kudos.ms.user.common.login.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserLoginRememberMeDetail / Edit / Row and UserLogLoginDetail
 *
 * @author K
 * @since 1.0.0
 */
internal class UserLoginResponseVosTest {

    private val t = LocalDateTime.of(2026, 6, 15, 7, 0)

    @Test
    fun rememberMeDetail() {
        val d = UserLoginRememberMeDetail()
        assertEquals("", d.id)
        assertNull(d.token)
        val full = d.copy(id = "rm-1", username = "alice", token = "tok", lastUsed = t)
        assertEquals("tok", full.token)
        assertEquals(t, full.lastUsed)
        assertEquals(full, full.copy())
        assertNotEquals(full, full.copy(token = "tok2"))
        assertTrue(full.toString().contains("alice"))
    }

    @Test
    fun rememberMeEdit() {
        val e = UserLoginRememberMeEdit(id = "rm-1", username = "alice", token = "tok")
        assertEquals("alice", e.username)
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(id = "rm-2"))
    }

    @Test
    fun rememberMeRow() {
        val r = UserLoginRememberMeRow()
        assertEquals("", r.id)
        val full = r.copy(id = "rm-1", username = "alice")
        assertEquals("alice", full.username)
        assertEquals(full, full.copy())
        assertNotEquals(full, full.copy(username = "bob"))
    }

    @Test
    fun logLoginDetail() {
        val d = UserLogLoginDetail()
        assertEquals("", d.id)
        assertNull(d.loginSuccess)
        val full = d.copy(
            id = "log-1",
            userId = "u-1",
            username = "alice",
            tenantId = "t-1",
            loginTime = t,
            loginIp = 10L,
            loginSuccess = true,
            failureReason = null,
            sessionId = "s-1",
        )
        assertEquals(true, full.loginSuccess)
        assertEquals(10L, full.loginIp)
        assertEquals("s-1", full.sessionId)
        assertEquals(full, full.copy())
        assertEquals(full.hashCode(), full.copy().hashCode())
        assertNotEquals(full, full.copy(loginSuccess = false))
        assertTrue(full.toString().contains("alice"))
    }

}
