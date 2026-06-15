package io.kudos.ms.sys.common.system.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysSystemEdit
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemEditTest {

    @Test
    fun defaults() {
        val e = SysSystemEdit()
        assertEquals("", e.id)
        assertEquals("", e.code)
        assertEquals("", e.name)
        assertTrue(e.subSystem)
        assertNull(e.parentCode)
        assertNull(e.remark)
        assertTrue(e.active)
        assertEquals(false, e.builtIn)
        assertNull(e.createUserId)
        assertNull(e.createUserName)
        assertNull(e.createTime)
        assertNull(e.updateUserId)
        assertNull(e.updateUserName)
        assertNull(e.updateTime)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysSystemEdit(
            id = "sys", code = "sys", name = "System", subSystem = false,
            parentCode = "root", remark = "r", active = false, builtIn = true,
            createUserId = "u-1", createUserName = "creator", createTime = now,
            updateUserId = "u-2", updateUserName = "updater", updateTime = now,
        )
        assertEquals("sys", e.id)
        assertEquals(false, e.subSystem)
        assertEquals("root", e.parentCode)
        assertEquals(false, e.active)
        assertEquals(true, e.builtIn)
        assertEquals(now, e.updateTime)
        assertEquals("creator", e.createUserName)
    }

    @Test
    fun dataClassContract() {
        val a = SysSystemEdit(id = "sys", code = "sys")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "other"))
        assertTrue(a.toString().contains("sys"))
    }

}
