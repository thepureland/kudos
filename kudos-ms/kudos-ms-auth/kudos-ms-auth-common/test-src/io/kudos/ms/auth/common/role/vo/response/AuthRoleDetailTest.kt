package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleDetail
 *
 * Covers all-default construction, full construction, IIdEntity id and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleDetailTest {

    @Test
    fun defaults() {
        val d = AuthRoleDetail()
        assertTrue(d is IIdEntity<*>)
        assertEquals("", d.id)
        assertNull(d.code)
        assertNull(d.name)
        assertNull(d.tenantId)
        assertNull(d.subsysCode)
        assertNull(d.parentId)
        assertNull(d.dataScope)
        assertNull(d.remark)
        assertNull(d.active)
        assertNull(d.builtIn)
        assertNull(d.approvalRequired)
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
        val d = AuthRoleDetail(
            id = "r-1", code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys",
            parentId = "p-1", dataScope = "ORG", remark = "r", active = true, builtIn = false,
            approvalRequired = true,
            createUserId = "cu", createUserName = "cn", createTime = t,
            updateUserId = "uu", updateUserName = "un", updateTime = t.plusDays(1),
        )
        assertEquals("r-1", d.id)
        assertEquals("ROLE1", d.code)
        assertEquals("p-1", d.parentId)
        assertEquals("ORG", d.dataScope)
        assertEquals(true, d.active)
        assertEquals(false, d.builtIn)
        assertEquals(true, d.approvalRequired)
        assertEquals(t, d.createTime)
        assertEquals(t.plusDays(1), d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = AuthRoleDetail(id = "r-1", code = "ROLE1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(dataScope = "ALL"))
        assertTrue(a.toString().contains("r-1"))
    }
}
