package io.kudos.ms.auth.common.exclusion.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleExclusionRow
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleExclusionRowTest {

    @Test
    fun defaults() {
        val r = AuthRoleExclusionRow()
        assertTrue(r is IIdEntity<*>)
        assertEquals("", r.id)
        assertNull(r.roleAId)
        assertNull(r.roleBId)
        assertNull(r.tenantId)
        assertNull(r.description)
        assertNull(r.createTime)
        assertNull(r.createUserName)
    }

    @Test
    fun fullArgs() {
        val t = LocalDateTime.of(2026, 6, 15, 10, 0)
        val r = AuthRoleExclusionRow(
            id = "e-1", roleAId = "a", roleBId = "b", tenantId = "t-1",
            description = "d", createTime = t, createUserName = "n1",
        )
        assertEquals("e-1", r.id)
        assertEquals("a", r.roleAId)
        assertEquals("b", r.roleBId)
        assertEquals("t-1", r.tenantId)
        assertEquals("d", r.description)
        assertEquals(t, r.createTime)
        assertEquals("n1", r.createUserName)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleExclusionRow(id = "e-1", roleAId = "a")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(roleBId = "x"))
        assertTrue(a.toString().contains("e-1"))
    }
}
