package io.kudos.ms.user.common.org.vo.request

import io.kudos.ms.user.common.org.vo.response.UserOrgRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserOrgQuery
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgQueryTest {

    @Test
    fun defaults() {
        val q = UserOrgQuery()
        assertNull(q.name)
        assertNull(q.shortName)
        assertNull(q.tenantId)
        assertNull(q.parentId)
        assertNull(q.orgTypeDictCode)
        assertNull(q.active)
        assertNull(q.builtIn)
    }

    @Test
    fun fullArgsAndReturnEntityClass() {
        val q = UserOrgQuery(
            name = "HQ",
            shortName = "hq",
            tenantId = "t-1",
            parentId = "p-1",
            orgTypeDictCode = "1",
            active = true,
            builtIn = false,
        )
        assertEquals("HQ", q.name)
        assertEquals("p-1", q.parentId)
        assertEquals(UserOrgRow::class, q.getReturnEntityClass())
    }

    @Test
    fun dataClassContract() {
        val a = UserOrgQuery(name = "HQ")
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(name = "Branch"))
        assertTrue(a.toString().contains("HQ"))
    }

}
