package io.kudos.ms.user.common.account.vo.request

import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserAccountQuery
 *
 * Covers default values (all null), full-args construction, the ListSearchPayload
 * getReturnEntityClass override and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountQueryTest {

    @Test
    fun defaults() {
        val q = UserAccountQuery()
        assertNull(q.username)
        assertNull(q.tenantId)
        assertNull(q.accountTypeDictCode)
        assertNull(q.accountStatusDictCode)
        assertNull(q.orgId)
        assertNull(q.supervisorId)
        assertNull(q.remark)
        assertNull(q.active)
        assertNull(q.builtIn)
    }

    @Test
    fun fullArgs() {
        val q = UserAccountQuery(
            username = "alice",
            tenantId = "t-1",
            accountTypeDictCode = "1",
            accountStatusDictCode = "0",
            orgId = "o-1",
            supervisorId = "s-1",
            remark = "r",
            active = true,
            builtIn = false,
        )
        assertEquals("alice", q.username)
        assertEquals("t-1", q.tenantId)
        assertEquals(true, q.active)
        assertEquals(false, q.builtIn)
    }

    @Test
    fun returnEntityClass() {
        assertEquals(UserAccountRow::class, UserAccountQuery().getReturnEntityClass())
    }

    @Test
    fun dataClassContract() {
        val a = UserAccountQuery(username = "alice")
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(username = "bob"))
        assertTrue(a.toString().contains("alice"))
    }

}
