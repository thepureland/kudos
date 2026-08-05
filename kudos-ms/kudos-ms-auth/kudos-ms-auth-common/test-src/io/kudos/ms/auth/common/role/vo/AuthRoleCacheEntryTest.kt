package io.kudos.ms.auth.common.role.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthRoleCacheEntry
 *
 * Covers full construction, defaulted nullable fields (parentId/dataScope/approvalRequired),
 * IIdEntity id, Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleCacheEntryTest {

    private fun entry() = AuthRoleCacheEntry(
        id = "r-1", code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys",
        parentId = "p-1", dataScope = "ORG", remark = "r", active = true, builtIn = false,
        approvalRequired = true,
        createUserId = "cu", createUserName = "cn", createTime = LocalDateTime.of(2026, 6, 15, 8, 0),
        updateUserId = "uu", updateUserName = "un", updateTime = LocalDateTime.of(2026, 6, 16, 8, 0),
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("r-1", e.id)
        assertEquals("ROLE1", e.code)
        assertEquals("Role 1", e.name)
        assertEquals("t-1", e.tenantId)
        assertEquals("sys", e.subsysCode)
        assertEquals("p-1", e.parentId)
        assertEquals("ORG", e.dataScope)
        assertEquals("r", e.remark)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
        assertEquals(true, e.approvalRequired)
        assertEquals("cu", e.createUserId)
        assertEquals(LocalDateTime.of(2026, 6, 16, 8, 0), e.updateTime)
    }

    @Test
    fun defaultedNullableFields() {
        val e = AuthRoleCacheEntry(
            id = "r-1", code = "ROLE1", name = "Role 1", tenantId = "t-1", subsysCode = "sys",
            remark = null, active = null, builtIn = null,
            createUserId = null, createUserName = null, createTime = null,
            updateUserId = null, updateUserName = null, updateTime = null,
        )
        assertNull(e.parentId)
        assertNull(e.dataScope)
        assertNull(e.approvalRequired)
        assertNull(e.remark)
        assertNull(e.active)
    }

    @Test
    fun isSerializable() {
        assertTrue(entry() is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(dataScope = "ALL"))
        assertTrue(a.toString().contains("r-1"))
    }
}
