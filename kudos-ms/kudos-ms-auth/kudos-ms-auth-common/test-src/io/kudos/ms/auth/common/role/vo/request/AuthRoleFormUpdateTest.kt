package io.kudos.ms.auth.common.role.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleFormUpdate
 *
 * Covers IIdEntity id + IAuthRoleFormBase property overrides, the defaulted
 * parentId/approvalRequired/dataScope, nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleFormUpdateTest {

    @Test
    fun defaultsForOptionalFields() {
        val f = AuthRoleFormUpdate(
            id = "r-1", code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys", remark = "r",
        )
        assertTrue(f is IIdEntity<*>)
        assertTrue(f is IAuthRoleFormBase)
        assertEquals("r-1", f.id)
        assertNull(f.parentId)
        assertEquals(false, f.approvalRequired)
        assertNull(f.dataScope)
    }

    @Test
    fun fullArgs() {
        val f = AuthRoleFormUpdate(
            id = "r-1", code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys",
            parentId = "p-1", approvalRequired = true, dataScope = "CUSTOM", remark = "r",
        )
        assertEquals("p-1", f.parentId)
        assertEquals(true, f.approvalRequired)
        assertEquals("CUSTOM", f.dataScope)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableFields() {
        val f = AuthRoleFormUpdate(
            id = "r-1", code = null, name = null, tenantId = null, subsysCode = null,
            approvalRequired = null, remark = null,
        )
        assertNull(f.code)
        assertNull(f.approvalRequired)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleFormUpdate(
            id = "r-1", code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys", remark = "r",
        )
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "r-2"))
        assertTrue(a.toString().contains("r-1"))
    }
}
