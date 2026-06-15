package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleResourceDetail
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleResourceDetailTest {

    @Test
    fun defaults() {
        val d = AuthRoleResourceDetail()
        assertTrue(d is IIdEntity<*>)
        assertEquals("", d.id)
        assertNull(d.roleId)
        assertNull(d.resourceId)
        assertNull(d.createUserId)
        assertNull(d.createUserName)
        assertNull(d.createTime)
        assertNull(d.updateUserId)
        assertNull(d.updateUserName)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgs() {
        val t = LocalDateTime.of(2026, 6, 15, 8, 0)
        val d = AuthRoleResourceDetail(
            id = "rr-1", roleId = "r-1", resourceId = "res-1",
            createUserId = "cu", createUserName = "cn", createTime = t,
            updateUserId = "uu", updateUserName = "un", updateTime = t.plusDays(1),
        )
        assertEquals("rr-1", d.id)
        assertEquals("r-1", d.roleId)
        assertEquals("res-1", d.resourceId)
        assertEquals(t, d.createTime)
        assertEquals(t.plusDays(1), d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleResourceDetail(id = "rr-1", roleId = "r-1", resourceId = "res-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(resourceId = "res-2"))
        assertTrue(a.toString().contains("rr-1"))
    }
}
