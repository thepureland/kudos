package io.kudos.ms.auth.common.group.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthGroupUserDetail
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupUserDetailTest {

    @Test
    fun defaults() {
        val d = AuthGroupUserDetail()
        assertTrue(d is IIdEntity<*>)
        assertEquals("", d.id)
        assertNull(d.groupId)
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
        val d = AuthGroupUserDetail(
            id = "gu-1", groupId = "grp-1", userId = "u-1",
            createUserId = "cu", createUserName = "cn", createTime = t,
            updateUserId = "uu", updateUserName = "un", updateTime = t.plusDays(1),
        )
        assertEquals("gu-1", d.id)
        assertEquals("grp-1", d.groupId)
        assertEquals("u-1", d.userId)
        assertEquals("cu", d.createUserId)
        assertEquals(t, d.createTime)
        assertEquals(t.plusDays(1), d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthGroupUserDetail(id = "gu-1", groupId = "grp-1", userId = "u-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(userId = "u-2"))
        assertTrue(a.toString().contains("gu-1"))
    }
}
