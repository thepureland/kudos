package io.kudos.ms.auth.common.exclusion.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleExclusionFormCreate
 *
 * Covers the default null description, full construction, nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleExclusionFormCreateTest {

    @Test
    fun defaultDescriptionIsNull() {
        val f = AuthRoleExclusionFormCreate(roleAId = "a", roleBId = "b", tenantId = "t-1")
        assertEquals("a", f.roleAId)
        assertEquals("b", f.roleBId)
        assertEquals("t-1", f.tenantId)
        assertNull(f.description)
    }

    @Test
    fun fullArgsAndNullableFields() {
        val f = AuthRoleExclusionFormCreate(
            roleAId = null, roleBId = null, tenantId = null,
            description = "Auditor may not also approve payments",
        )
        assertNull(f.roleAId)
        assertNull(f.roleBId)
        assertNull(f.tenantId)
        assertEquals("Auditor may not also approve payments", f.description)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleExclusionFormCreate(roleAId = "a", roleBId = "b", tenantId = "t-1", description = "d")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(roleBId = "c"))
        assertTrue(a.toString().contains("d"))
    }
}
