package io.kudos.ms.sys.common.accessrule.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for SysAccessRuleFormUpdate
 *
 * Covers property accessors (IIdEntity id + ISysAccessRuleFormBase overrides) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleFormUpdateTest {

    @Test
    fun properties() {
        val form = SysAccessRuleFormUpdate(
            id = "id-1",
            accessRuleTypeDictCode = "3",
            remark = "note",
        )
        assertEquals("id-1", form.id)
        assertEquals("3", form.accessRuleTypeDictCode)
        assertEquals("note", form.remark)
    }

    @Test
    fun dataClassContract() {
        val a = SysAccessRuleFormUpdate("id-1", "3", null)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(remark = "x"))
        assertTrue(a.toString().contains("id-1"))
    }

}
