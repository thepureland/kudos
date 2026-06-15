package io.kudos.ms.sys.common.i18n.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysI18nRow
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nRowTest {

    @Test
    fun defaults() {
        val r = SysI18nRow()
        assertEquals("", r.id)
        assertEquals("", r.locale)
        assertEquals("", r.atomicServiceCode)
        assertEquals("", r.i18nTypeDictCode)
        assertEquals("", r.namespace)
        assertEquals("", r.key)
        assertEquals("", r.value)
        assertNull(r.remark)
        assertEquals(true, r.active)
        assertEquals(true, r.builtIn)
    }

    @Test
    fun fullArgs() {
        val r = SysI18nRow(
            id = "i-1",
            locale = "zh_CN",
            atomicServiceCode = "sys",
            i18nTypeDictCode = "ui",
            namespace = "common",
            key = "ok",
            value = "确定",
            remark = "r",
            active = false,
            builtIn = false,
        )
        assertEquals("i-1", r.id)
        assertEquals("确定", r.value)
        assertEquals("r", r.remark)
        assertEquals(false, r.active)
        assertEquals(false, r.builtIn)
    }

    @Test
    fun dataClassContract() {
        val a = SysI18nRow(id = "i-1", key = "ok")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("i-1"))
    }

}
