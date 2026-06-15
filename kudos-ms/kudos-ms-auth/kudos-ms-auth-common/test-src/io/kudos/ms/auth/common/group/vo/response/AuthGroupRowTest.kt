package io.kudos.ms.auth.common.group.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthGroupRow
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupRowTest {

    @Test
    fun defaults() {
        val r = AuthGroupRow()
        assertTrue(r is IIdEntity<*>)
        assertEquals("", r.id)
        assertNull(r.code)
        assertNull(r.name)
        assertNull(r.tenantId)
        assertNull(r.subsysCode)
        assertNull(r.remark)
        assertNull(r.active)
        assertNull(r.builtIn)
        assertNull(r.createUserId)
        assertNull(r.createUserName)
        assertNull(r.createTime)
        assertNull(r.updateUserId)
        assertNull(r.updateUserName)
        assertNull(r.updateTime)
    }

    @Test
    fun fullArgs() {
        val t = LocalDateTime.of(2026, 6, 15, 8, 0)
        val r = AuthGroupRow(
            id = "grp-1", code = "G1", name = "Group 1", tenantId = "t-1", subsysCode = "sys",
            remark = "r", active = true, builtIn = false,
            createUserId = "cu", createUserName = "cn", createTime = t,
            updateUserId = "uu", updateUserName = "un", updateTime = t.plusDays(1),
        )
        assertEquals("grp-1", r.id)
        assertEquals("G1", r.code)
        assertEquals(true, r.active)
        assertEquals(false, r.builtIn)
        assertEquals(t, r.createTime)
        assertEquals(t.plusDays(1), r.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthGroupRow(id = "grp-1", code = "G1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(builtIn = true))
        assertTrue(a.toString().contains("grp-1"))
    }
}
