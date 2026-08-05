package io.kudos.ms.sys.common.i18n.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysI18nEdit
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nEditTest {

    @Test
    fun defaults() {
        val e = SysI18nEdit()
        assertEquals("", e.id)
        assertEquals("", e.locale)
        assertEquals("", e.atomicServiceCode)
        assertEquals("", e.i18nTypeDictCode)
        assertEquals("", e.namespace)
        assertEquals("", e.key)
        assertEquals("", e.value)
        assertNull(e.remark)
        assertEquals(true, e.active)
        assertEquals(true, e.builtIn)
        assertNull(e.createTime)
        assertNull(e.updateTime)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysI18nEdit(
            id = "i-1",
            locale = "en_US",
            atomicServiceCode = "sys",
            i18nTypeDictCode = "ui",
            namespace = "common",
            key = "ok",
            value = "OK",
            remark = "r",
            active = false,
            builtIn = false,
            createUserId = "u1",
            createUserName = "creator",
            createTime = now,
            updateUserId = "u2",
            updateUserName = "updater",
            updateTime = now,
        )
        assertTrue(e is IIdEntity<*>)
        assertEquals("i-1", e.id)
        assertEquals("OK", e.value)
        assertEquals(now, e.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysI18nEdit(id = "i-1", key = "ok")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(value = "x"))
        assertTrue(a.toString().contains("i-1"))
    }

}
