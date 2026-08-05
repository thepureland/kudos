package io.kudos.ms.user.common.account.vo.request

import io.kudos.ms.user.common.account.vo.response.UserAccountThirdRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserAccountThirdQuery
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountThirdQueryTest {

    @Test
    fun defaults() {
        val q = UserAccountThirdQuery()
        assertNull(q.userId)
        assertNull(q.accountProviderDictCode)
        assertNull(q.accountProviderIssuer)
        assertNull(q.subject)
        assertNull(q.unionId)
        assertNull(q.externalDisplayName)
        assertNull(q.externalEmail)
        assertNull(q.avatarUrl)
        assertNull(q.tenantId)
        assertNull(q.active)
        assertNull(q.builtIn)
    }

    @Test
    fun fullArgsAndReturnEntityClass() {
        val q = UserAccountThirdQuery(
            userId = "u-1",
            accountProviderDictCode = "wechat",
            subject = "open-id",
            tenantId = "t-1",
            active = true,
        )
        assertEquals("u-1", q.userId)
        assertEquals("wechat", q.accountProviderDictCode)
        assertEquals("open-id", q.subject)
        assertEquals(UserAccountThirdRow::class, q.getReturnEntityClass())
    }

    @Test
    fun dataClassContract() {
        val a = UserAccountThirdQuery(userId = "u-1")
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(subject = "x"))
        assertTrue(a.toString().contains("u-1"))
    }

}
