package io.kudos.ms.sys.common.i18n.vo.request

import io.kudos.ms.sys.common.i18n.vo.request.ISysI18nFormBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysI18nFormCreate
 *
 * Covers property accessors (ISysI18nFormBase overrides), nullable remark and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nFormCreateTest {

    private fun form(remark: String? = "r") = SysI18nFormCreate(
        locale = "zh_CN",
        atomicServiceCode = "sys",
        i18nTypeDictCode = "ui",
        namespace = "common",
        key = "ok",
        value = "确定",
        remark = remark,
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysI18nFormBase)
        assertEquals("zh_CN", f.locale)
        assertEquals("sys", f.atomicServiceCode)
        assertEquals("ui", f.i18nTypeDictCode)
        assertEquals("common", f.namespace)
        assertEquals("ok", f.key)
        assertEquals("确定", f.value)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableRemark() {
        assertNull(form(remark = null).remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(key = "cancel"))
        assertTrue(a.toString().contains("common"))
    }

}
