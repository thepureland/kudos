package io.kudos.ms.user.common.login.vo.request

import io.kudos.ms.user.common.login.vo.response.UserLoginRememberMeRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserLoginRememberMeQuery
 *
 * @author K
 * @since 1.0.0
 */
internal class UserLoginRememberMeQueryTest {

    @Test
    fun defaultAndReturnEntityClass() {
        val q = UserLoginRememberMeQuery()
        assertNull(q.username)
        assertEquals(UserLoginRememberMeRow::class, q.getReturnEntityClass())
    }

    @Test
    fun fullArgsAndDataClassContract() {
        val a = UserLoginRememberMeQuery(username = "alice")
        assertEquals("alice", a.username)
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(username = "bob"))
        assertTrue(a.toString().contains("alice"))
    }

}
