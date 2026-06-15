package io.kudos.ms.auth.common.group.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthGroupFormUpdate
 *
 * Covers IIdEntity id + IAuthGroupFormBase property overrides, nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupFormUpdateTest {

    @Test
    fun properties() {
        val f = AuthGroupFormUpdate(
            id = "grp-1", code = "G1", name = "Group 1", tenantId = "t-1", subsysCode = "sys", remark = "r",
        )
        assertTrue(f is IIdEntity<*>)
        assertTrue(f is IAuthGroupFormBase)
        assertEquals("grp-1", f.id)
        assertEquals("G1", f.code)
        assertEquals("Group 1", f.name)
        assertEquals("t-1", f.tenantId)
        assertEquals("sys", f.subsysCode)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableFields() {
        val f = AuthGroupFormUpdate(id = "grp-1", code = null, name = null, tenantId = null, subsysCode = null, remark = null)
        assertEquals("grp-1", f.id)
        assertNull(f.code)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = AuthGroupFormUpdate("grp-1", "G1", "Group 1", "t-1", "sys", "r")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "grp-2"))
        assertTrue(a.toString().contains("grp-1"))
    }
}
