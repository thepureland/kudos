package io.kudos.ms.auth.common.role.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleFormCreate
 *
 * Covers IAuthRoleFormBase property overrides, the defaulted parentId/approvalRequired/dataScope,
 * nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleFormCreateTest {

    @Test
    fun defaultsForOptionalFields() {
        val f = AuthRoleFormCreate(
            code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys", remark = "r",
        )
        assertTrue(f is IAuthRoleFormBase)
        assertNull(f.parentId)
        assertEquals(false, f.approvalRequired)
        assertNull(f.dataScope)
    }

    @Test
    fun fullArgs() {
        val f = AuthRoleFormCreate(
            code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys",
            parentId = "p-1", approvalRequired = true, dataScope = "ORG", remark = "r",
        )
        assertEquals("ROLE1", f.code)
        assertEquals("Role 1", f.name)
        assertEquals("t-1", f.tenantId)
        assertEquals("sys", f.subsysCode)
        assertEquals("p-1", f.parentId)
        assertEquals(true, f.approvalRequired)
        assertEquals("ORG", f.dataScope)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableFields() {
        val f = AuthRoleFormCreate(
            code = null, name = null, tenantId = null, subsysCode = null,
            approvalRequired = null, remark = null,
        )
        assertNull(f.code)
        assertNull(f.name)
        assertNull(f.tenantId)
        assertNull(f.subsysCode)
        assertNull(f.approvalRequired)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleFormCreate(
            code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys", remark = "r",
        )
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(approvalRequired = true))
        assertFalse(a == a.copy(dataScope = "ALL"))
        assertTrue(a.toString().contains("ROLE1"))
    }
}
