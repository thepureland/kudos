package io.kudos.ms.user.common.contact.vo.request

import io.kudos.ms.user.common.contact.vo.response.UserContactWayRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserContactWayQuery
 *
 * @author K
 * @since 1.0.0
 */
internal class UserContactWayQueryTest {

    @Test
    fun defaults() {
        val q = UserContactWayQuery()
        assertNull(q.userId)
        assertNull(q.contactWayDictCode)
        assertNull(q.contactWayValue)
        assertNull(q.contactWayStatusDictCode)
        assertNull(q.active)
        assertNull(q.builtIn)
    }

    @Test
    fun fullArgsAndReturnEntityClass() {
        val q = UserContactWayQuery(
            userId = "u-1",
            contactWayDictCode = "email",
            contactWayValue = "a@b.com",
            contactWayStatusDictCode = "1",
            active = true,
            builtIn = false,
        )
        assertEquals("u-1", q.userId)
        assertEquals("email", q.contactWayDictCode)
        assertEquals(UserContactWayRow::class, q.getReturnEntityClass())
    }

    @Test
    fun dataClassContract() {
        val a = UserContactWayQuery(userId = "u-1")
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(contactWayDictCode = "phone"))
        assertTrue(a.toString().contains("u-1"))
    }

}
