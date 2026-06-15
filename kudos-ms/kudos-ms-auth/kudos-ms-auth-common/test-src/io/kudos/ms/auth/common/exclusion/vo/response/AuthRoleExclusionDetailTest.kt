package io.kudos.ms.auth.common.exclusion.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleExclusionDetail
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleExclusionDetailTest {

    @Test
    fun defaults() {
        val d = AuthRoleExclusionDetail()
        assertTrue(d is IIdEntity<*>)
        assertEquals("", d.id)
        assertNull(d.roleAId)
        assertNull(d.roleBId)
        assertNull(d.tenantId)
        assertNull(d.description)
        assertNull(d.createUserId)
        assertNull(d.createUserName)
        assertNull(d.createTime)
        assertNull(d.updateUserId)
        assertNull(d.updateUserName)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgs() {
        val t = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = AuthRoleExclusionDetail(
            id = "e-1", roleAId = "a", roleBId = "b", tenantId = "t-1", description = "d",
            createUserId = "u1", createUserName = "n1", createTime = t,
            updateUserId = "u2", updateUserName = "n2", updateTime = t.plusDays(1),
        )
        assertEquals("e-1", d.id)
        assertEquals("a", d.roleAId)
        assertEquals("b", d.roleBId)
        assertEquals("t-1", d.tenantId)
        assertEquals("d", d.description)
        assertEquals("u1", d.createUserId)
        assertEquals("n1", d.createUserName)
        assertEquals(t, d.createTime)
        assertEquals("u2", d.updateUserId)
        assertEquals("n2", d.updateUserName)
        assertEquals(t.plusDays(1), d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleExclusionDetail(id = "e-1", roleAId = "a")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(roleAId = "x"))
        assertTrue(a.toString().contains("e-1"))
    }
}
