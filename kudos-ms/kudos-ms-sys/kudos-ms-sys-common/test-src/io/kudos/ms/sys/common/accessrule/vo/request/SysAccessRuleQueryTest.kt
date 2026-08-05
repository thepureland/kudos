package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleQuery
 *
 * Covers default values, full-args construction, ListSearchPayload overrides
 * (getReturnEntityClass / isUnpagedSearchAllowed) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = SysAccessRuleQuery()
        assertNull(q.tenantId)
        assertNull(q.systemCode)
        assertNull(q.accessRuleTypeDictCode)
        assertNull(q.active)
    }

    @Test
    fun fullArgsConstruction() {
        val q = SysAccessRuleQuery(
            tenantId = "t-1",
            systemCode = "sys-a",
            accessRuleTypeDictCode = "4",
            active = false,
        )
        assertEquals("t-1", q.tenantId)
        assertEquals("sys-a", q.systemCode)
        assertEquals("4", q.accessRuleTypeDictCode)
        assertEquals(false, q.active)
    }

    @Test
    fun payloadOverrides() {
        val q = SysAccessRuleQuery()
        assertEquals(SysAccessRuleRow::class, q.getReturnEntityClass())
        assertTrue(q.isUnpagedSearchAllowed())
    }

    @Test
    fun dataClassContract() {
        val a = SysAccessRuleQuery(tenantId = "t-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = true))
        assertTrue(a.toString().contains("t-1"))
    }

}
