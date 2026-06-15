package io.kudos.ms.sys.common.locale.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysLocaleEdit
 *
 * Covers default values (builtIn defaults to false), full-args construction,
 * IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysLocaleEditTest {

    @Test
    fun defaults() {
        val e = SysLocaleEdit()
        assertEquals("", e.id)
        assertEquals("", e.code)
        assertEquals("", e.displayName)
        assertEquals("", e.englishName)
        assertEquals(0, e.sortNo)
        assertNull(e.remark)
        assertEquals(true, e.active)
        assertEquals(false, e.builtIn)
        assertNull(e.createTime)
        assertNull(e.updateTime)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysLocaleEdit(
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
        assertTrue(e is IIdEntity<*>)
        assertEquals("l-1", e.id)
        assertEquals(now, e.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysLocaleEdit(id = "l-1", code = "zh_CN")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(sortNo = 9))
        assertTrue(a.toString().contains("l-1"))
    }

}
