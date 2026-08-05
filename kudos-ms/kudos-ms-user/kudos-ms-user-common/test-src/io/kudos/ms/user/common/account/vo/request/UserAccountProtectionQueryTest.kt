package io.kudos.ms.user.common.account.vo.request

import io.kudos.ms.user.common.account.vo.response.UserAccountProtectionRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserAccountProtectionQuery
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountProtectionQueryTest {

    @Test
    fun defaults() {
        val q = UserAccountProtectionQuery()
        assertNull(q.userId)
        assertNull(q.active)
        assertNull(q.builtIn)
    }

    @Test
    fun fullArgsAndReturnEntityClass() {
        val q = UserAccountProtectionQuery(userId = "u-1", active = true, builtIn = false)
        assertEquals("u-1", q.userId)
        assertEquals(true, q.active)
        assertEquals(false, q.builtIn)
        assertEquals(UserAccountProtectionRow::class, q.getReturnEntityClass())
    }

    @Test
    fun dataClassContract() {
        val a = UserAccountProtectionQuery(userId = "u-1")
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(userId = "u-2"))
        assertTrue(a.toString().contains("u-1"))
    }

}
