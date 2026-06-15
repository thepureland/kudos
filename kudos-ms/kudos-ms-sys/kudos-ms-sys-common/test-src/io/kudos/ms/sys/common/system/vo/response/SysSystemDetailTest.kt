package io.kudos.ms.sys.common.system.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysSystemDetail
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysSystemDetailTest {

    @Test
    fun defaults() {
        val d = SysSystemDetail()
        assertEquals("", d.id)
        assertEquals("", d.code)
        assertEquals("", d.name)
        assertTrue(d.subSystem)
        assertNull(d.parentCode)
        assertNull(d.remark)
        assertTrue(d.active)
        assertEquals(false, d.builtIn)
        assertNull(d.createUserId)
        assertNull(d.createUserName)
        assertNull(d.createTime)
        assertNull(d.updateUserId)
        assertNull(d.updateUserName)
        assertNull(d.updateTime)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysSystemDetail(
            id = "sys", code = "sys", name = "System", subSystem = false,
            parentCode = "root", remark = "r", active = false, builtIn = true,
            createUserId = "u-1", createUserName = "creator", createTime = now,
            updateUserId = "u-2", updateUserName = "updater", updateTime = now,
        )
        assertEquals("sys", d.id)
        assertEquals(false, d.subSystem)
        assertEquals("root", d.parentCode)
        assertEquals(false, d.active)
        assertEquals(true, d.builtIn)
        assertEquals(now, d.createTime)
        assertEquals("updater", d.updateUserName)
    }

    @Test
    fun dataClassContract() {
        val a = SysSystemDetail(id = "sys", code = "sys")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "other"))
        assertTrue(a.toString().contains("sys"))
    }

}
