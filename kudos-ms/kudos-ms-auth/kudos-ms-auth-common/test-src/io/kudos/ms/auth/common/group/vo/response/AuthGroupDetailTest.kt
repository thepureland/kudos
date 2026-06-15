package io.kudos.ms.auth.common.group.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthGroupDetail
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupDetailTest {

    @Test
    fun defaults() {
        val d = AuthGroupDetail()
        assertTrue(d is IIdEntity<*>)
        assertEquals("", d.id)
        assertNull(d.code)
        assertNull(d.name)
        assertNull(d.tenantId)
        assertNull(d.subsysCode)
        assertNull(d.remark)
        assertNull(d.active)
        assertNull(d.builtIn)
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
        val d = AuthGroupDetail(
            id = "grp-1", code = "G1", name = "Group 1", tenantId = "t-1", subsysCode = "sys",
            remark = "r", active = true, builtIn = false,
            createUserId = "cu", createUserName = "cn", createTime = t,
            updateUserId = "uu", updateUserName = "un", updateTime = t.plusDays(1),
        )
        assertEquals("grp-1", d.id)
        assertEquals("G1", d.code)
        assertEquals("Group 1", d.name)
        assertEquals("t-1", d.tenantId)
        assertEquals("sys", d.subsysCode)
        assertEquals("r", d.remark)
        assertEquals(true, d.active)
        assertEquals(false, d.builtIn)
        assertEquals("cu", d.createUserId)
        assertEquals("cn", d.createUserName)
        assertEquals(t, d.createTime)
        assertEquals("uu", d.updateUserId)
        assertEquals("un", d.updateUserName)
        assertEquals(t.plusDays(1), d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthGroupDetail(id = "grp-1", code = "G1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "G2"))
        assertTrue(a.toString().contains("grp-1"))
    }
}
