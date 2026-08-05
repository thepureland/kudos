package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleUserDetail
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleUserDetailTest {

    @Test
    fun defaults() {
        val d = AuthRoleUserDetail()
        assertTrue(d is IIdEntity<*>)
        assertEquals("", d.id)
        assertNull(d.roleId)
        assertNull(d.userId)
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
        val d = AuthRoleUserDetail(
            id = "ru-1", roleId = "r-1", userId = "u-1",
            createUserId = "cu", createUserName = "cn", createTime = t,
            updateUserId = "uu", updateUserName = "un", updateTime = t.plusDays(1),
        )
        assertEquals("ru-1", d.id)
        assertEquals("r-1", d.roleId)
        assertEquals("u-1", d.userId)
        assertEquals(t, d.createTime)
        assertEquals(t.plusDays(1), d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleUserDetail(id = "ru-1", roleId = "r-1", userId = "u-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(userId = "u-2"))
        assertTrue(a.toString().contains("ru-1"))
    }
}
