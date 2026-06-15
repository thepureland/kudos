package io.kudos.ms.sys.common.i18n.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysI18nFormUpdate
 *
 * Covers property accessors (id + ISysI18nFormBase overrides), IIdEntity contract,
 * nullable remark and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysI18nFormUpdateTest {

    private fun form(remark: String? = "r") = SysI18nFormUpdate(
        id = "i-1",
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
        assertTrue(f is IIdEntity<*>)
        assertEquals("i-1", f.id)
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
        assertNotEquals(a, a.copy(id = "i-2"))
        assertTrue(a.toString().contains("i-1"))
    }

}
