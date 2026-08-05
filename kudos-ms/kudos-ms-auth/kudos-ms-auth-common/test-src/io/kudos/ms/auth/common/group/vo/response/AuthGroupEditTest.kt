package io.kudos.ms.auth.common.group.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthGroupEdit
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupEditTest {

    @Test
    fun defaults() {
        val e = AuthGroupEdit()
        assertTrue(e is IIdEntity<*>)
        assertEquals("", e.id)
        assertNull(e.code)
        assertNull(e.name)
        assertNull(e.tenantId)
        assertNull(e.subsysCode)
        assertNull(e.remark)
    }

    @Test
    fun fullArgs() {
        val e = AuthGroupEdit(
            id = "grp-1", code = "G1", name = "Group 1", tenantId = "t-1", subsysCode = "sys", remark = "r",
        )
        assertEquals("grp-1", e.id)
        assertEquals("G1", e.code)
        assertEquals("Group 1", e.name)
        assertEquals("t-1", e.tenantId)
        assertEquals("sys", e.subsysCode)
        assertEquals("r", e.remark)
    }

    @Test
    fun dataClassContract() {
        val a = AuthGroupEdit(id = "grp-1", code = "G1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "x"))
        assertTrue(a.toString().contains("grp-1"))
    }
}
