package io.kudos.ms.sys.common.locale.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysLocaleRow
 *
 * Covers default values (builtIn defaults to false), full-args construction
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleRowTest {

    @Test
    fun defaults() {
        val r = SysLocaleRow()
        assertEquals("", r.id)
        assertEquals("", r.code)
        assertEquals("", r.displayName)
        assertEquals("", r.englishName)
        assertEquals(0, r.sortNo)
        assertNull(r.remark)
        assertEquals(true, r.active)
        assertEquals(false, r.builtIn)
        assertNull(r.createTime)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val r = SysLocaleRow(
            id = "l-1",
            code = "zh_CN",
            displayName = "简体中文",
            englishName = "Simplified Chinese",
            sortNo = 5,
            remark = "r",
            active = false,
            builtIn = true,
            createTime = now,
        )
        assertEquals("l-1", r.id)
        assertEquals(5, r.sortNo)
        assertEquals(now, r.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysLocaleRow(id = "l-1", code = "zh_CN")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("l-1"))
    }

}
