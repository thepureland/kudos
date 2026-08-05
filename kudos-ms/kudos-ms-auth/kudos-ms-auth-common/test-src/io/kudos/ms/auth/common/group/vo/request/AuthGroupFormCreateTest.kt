package io.kudos.ms.auth.common.group.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthGroupFormCreate
 *
 * Covers IAuthGroupFormBase property overrides, nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupFormCreateTest {

    @Test
    fun properties() {
        val f = AuthGroupFormCreate(
            code = "G1", name = "Group 1", tenantId = "t-1", subsysCode = "sys", remark = "r",
        )
        assertTrue(f is IAuthGroupFormBase)
        assertEquals("G1", f.code)
        assertEquals("Group 1", f.name)
        assertEquals("t-1", f.tenantId)
        assertEquals("sys", f.subsysCode)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableFields() {
        val f = AuthGroupFormCreate(code = null, name = null, tenantId = null, subsysCode = null, remark = null)
        assertNull(f.code)
        assertNull(f.name)
        assertNull(f.tenantId)
        assertNull(f.subsysCode)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = AuthGroupFormCreate("G1", "Group 1", "t-1", "sys", "r")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "G2"))
        assertTrue(a.toString().contains("G1"))
    }
}
