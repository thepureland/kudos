package io.kudos.ms.sys.common.accessrule.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleFormCreate
 *
 * Covers property accessors (including ISysAccessRuleFormBase overrides) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleFormCreateTest {

    @Test
    fun properties() {
        val form = SysAccessRuleFormCreate(
            tenantId = "t-1",
            systemCode = "sys-a",
            accessRuleTypeDictCode = "2",
            remark = null,
        )
        assertEquals("t-1", form.tenantId)
        assertEquals("sys-a", form.systemCode)
        assertEquals("2", form.accessRuleTypeDictCode)
        assertNull(form.remark)
    }

    @Test
    fun dataClassContract() {
        val a = SysAccessRuleFormCreate("t-1", "sys-a", "2", "r")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(systemCode = "sys-b"))
        assertTrue(a.toString().contains("sys-a"))
    }

}
