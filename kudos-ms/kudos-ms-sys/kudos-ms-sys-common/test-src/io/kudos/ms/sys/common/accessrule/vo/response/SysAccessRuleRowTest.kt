package io.kudos.ms.sys.common.accessrule.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleRow
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleRowTest {

    @Test
    fun defaults() {
        val r = SysAccessRuleRow()
        assertEquals("", r.id)
        assertNull(r.tenantId)
        assertNull(r.systemCode)
        assertNull(r.accessRuleTypeDictCode)
    }

    @Test
    fun fullArgsConstruction() {
        val r = SysAccessRuleRow("id-1", "t-1", "sys-a", "4")
        assertEquals("id-1", r.id)
        assertEquals("t-1", r.tenantId)
        assertEquals("sys-a", r.systemCode)
        assertEquals("4", r.accessRuleTypeDictCode)
    }

    @Test
    fun dataClassContract() {
        val a = SysAccessRuleRow("id-1", "t-1", "sys-a", "4")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(tenantId = null))
        assertTrue(a.toString().contains("sys-a"))
    }

}
