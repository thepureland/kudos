package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleEdit
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleEditTest {

    @Test
    fun defaults() {
        val e = AuthRoleEdit()
        assertTrue(e is IIdEntity<*>)
        assertEquals("", e.id)
        assertNull(e.code)
        assertNull(e.name)
        assertNull(e.tenantId)
        assertNull(e.subsysCode)
        assertNull(e.parentId)
        assertNull(e.dataScope)
        assertNull(e.remark)
    }

    @Test
    fun fullArgs() {
        val e = AuthRoleEdit(
            id = "r-1", code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys",
            parentId = "p-1", dataScope = "ORG", remark = "r",
        )
        assertEquals("r-1", e.id)
        assertEquals("ROLE1", e.code)
        assertEquals("p-1", e.parentId)
        assertEquals("ORG", e.dataScope)
        assertEquals("r", e.remark)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleEdit(id = "r-1", code = "ROLE1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(dataScope = "SELF"))
        assertTrue(a.toString().contains("r-1"))
    }
}
