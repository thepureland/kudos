package io.kudos.ms.sys.common.locale.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysLocaleDetail
 *
 * Covers default values (builtIn defaults to false), full-args construction,
 * IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleDetailTest {

    @Test
    fun defaults() {
        val d = SysLocaleDetail()
        assertEquals("", d.id)
        assertEquals("", d.code)
        assertEquals("", d.displayName)
        assertEquals("", d.englishName)
        assertEquals(0, d.sortNo)
        assertNull(d.remark)
        assertEquals(true, d.active)
        assertEquals(false, d.builtIn)
        assertNull(d.createTime)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysLocaleDetail(
            id = "l-1",
            code = "zh_CN",
            displayName = "简体中文",
            englishName = "Simplified Chinese",
            sortNo = 5,
            remark = "r",
            active = false,
            builtIn = true,
            createUserId = "u1",
            createUserName = "creator",
            createTime = now,
            updateUserId = "u2",
            updateUserName = "updater",
            updateTime = now,
        )
        assertTrue(d is IIdEntity<*>)
        assertEquals("l-1", d.id)
        assertEquals(5, d.sortNo)
        assertEquals(true, d.builtIn)
        assertEquals(now, d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysLocaleDetail(id = "l-1", code = "zh_CN")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("l-1"))
    }

}
