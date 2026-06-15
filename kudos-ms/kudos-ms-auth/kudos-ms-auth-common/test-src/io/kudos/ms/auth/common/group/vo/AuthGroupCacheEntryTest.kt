package io.kudos.ms.auth.common.group.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for AuthGroupCacheEntry
 *
 * Covers full construction, nullable fields, IIdEntity id, Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupCacheEntryTest {

    private fun entry() = AuthGroupCacheEntry(
        id = "grp-1", code = "G1", name = "Group 1", tenantId = "t-1", subsysCode = "sys",
        remark = "r", active = true, builtIn = false,
        createUserId = "cu", createUserName = "cn", createTime = LocalDateTime.of(2026, 6, 15, 8, 0),
        updateUserId = "uu", updateUserName = "un", updateTime = LocalDateTime.of(2026, 6, 16, 8, 0),
    )

    @Test
    fun properties() {
        val e = entry()
        assertTrue(e is IIdEntity<*>)
        assertEquals("grp-1", e.id)
        assertEquals("G1", e.code)
        assertEquals("Group 1", e.name)
        assertEquals("t-1", e.tenantId)
        assertEquals("sys", e.subsysCode)
        assertEquals("r", e.remark)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
        assertEquals("cu", e.createUserId)
        assertEquals("cn", e.createUserName)
        assertEquals(LocalDateTime.of(2026, 6, 15, 8, 0), e.createTime)
        assertEquals("uu", e.updateUserId)
        assertEquals("un", e.updateUserName)
        assertEquals(LocalDateTime.of(2026, 6, 16, 8, 0), e.updateTime)
    }

    @Test
    fun nullableFields() {
        val e = entry().copy(
            code = null, name = null, tenantId = null, subsysCode = null, remark = null,
            active = null, builtIn = null, createUserId = null, createUserName = null,
            createTime = null, updateUserId = null, updateUserName = null, updateTime = null,
        )
        assertEquals("grp-1", e.id)
        assertNull(e.code)
        assertNull(e.active)
        assertNull(e.builtIn)
        assertNull(e.updateTime)
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
        assertNotEquals(a, a.copy(code = "G2"))
        assertTrue(a.toString().contains("grp-1"))
    }
}
