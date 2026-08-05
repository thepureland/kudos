package io.kudos.ms.sys.common.i18n.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysI18nDetail
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nDetailTest {

    @Test
    fun defaults() {
        val d = SysI18nDetail()
        assertEquals("", d.id)
        assertEquals("", d.locale)
        assertEquals("", d.atomicServiceCode)
        assertEquals("", d.i18nTypeDictCode)
        assertEquals("", d.namespace)
        assertEquals("", d.key)
        assertEquals("", d.value)
        assertNull(d.remark)
        assertEquals(true, d.active)
        assertEquals(true, d.builtIn)
        assertNull(d.createUserId)
        assertNull(d.createUserName)
        assertNull(d.createTime)
        assertNull(d.updateUserId)
        assertNull(d.updateUserName)
        assertNull(d.updateTime)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysI18nDetail(
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
            createUserId = "u1",
            createUserName = "creator",
            createTime = now,
            updateUserId = "u2",
            updateUserName = "updater",
            updateTime = now,
        )
        assertTrue(d is IIdEntity<*>)
        assertEquals("i-1", d.id)
        assertEquals("确定", d.value)
        assertEquals("r", d.remark)
        assertEquals(false, d.active)
        assertEquals(false, d.builtIn)
        assertEquals("creator", d.createUserName)
        assertEquals(now, d.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysI18nDetail(id = "i-1", key = "ok")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("i-1"))
    }

}
