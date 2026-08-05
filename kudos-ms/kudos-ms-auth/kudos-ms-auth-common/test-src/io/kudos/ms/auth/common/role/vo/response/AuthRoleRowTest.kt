package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleRow
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleRowTest {

    @Test
    fun defaults() {
        val r = AuthRoleRow()
        assertTrue(r is IIdEntity<*>)
        assertEquals("", r.id)
        assertNull(r.code)
        assertNull(r.name)
        assertNull(r.tenantId)
        assertNull(r.subsysCode)
        assertNull(r.parentId)
        assertNull(r.dataScope)
        assertNull(r.remark)
        assertNull(r.active)
        assertNull(r.builtIn)
        assertNull(r.approvalRequired)
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
        val r = AuthRoleRow(
            id = "r-1", code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys",
            parentId = "p-1", dataScope = "ORG", remark = "r", active = true, builtIn = false,
            approvalRequired = true,
            createUserId = "cu", createUserName = "cn", createTime = t,
            updateUserId = "uu", updateUserName = "un", updateTime = t.plusDays(1),
        )
        assertEquals("r-1", r.id)
        assertEquals("ROLE1", r.code)
        assertEquals("p-1", r.parentId)
        assertEquals("ORG", r.dataScope)
        assertEquals(true, r.active)
        assertEquals(false, r.builtIn)
        assertEquals(true, r.approvalRequired)
        assertEquals(t, r.createTime)
        assertEquals(t.plusDays(1), r.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleRow(id = "r-1", code = "ROLE1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(approvalRequired = true))
        assertTrue(a.toString().contains("r-1"))
    }
}
