package io.kudos.ms.sys.common.tenant.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysTenantFormUpdate
 *
 * Covers id (IIdEntity) plus ISysTenantFormBase property accessors, the default
 * empty subSystemCodes, nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantFormUpdateTest {

    private fun form() = SysTenantFormUpdate(
        id = "00000000-0000-0000-0000-000000000001",
        name = "Tenant One",
        subSystemCodes = setOf("sys"),
        timezone = "Asia/Shanghai",
        defaultLanguageCode = "zh-CN",
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysTenantFormBase)
        assertTrue(f is IIdEntity<*>)
        assertEquals("00000000-0000-0000-0000-000000000001", f.id)
        assertEquals("Tenant One", f.name)
        assertEquals(setOf("sys"), f.subSystemCodes)
        assertEquals("Asia/Shanghai", f.timezone)
        assertEquals("zh-CN", f.defaultLanguageCode)
        assertEquals("r", f.remark)
    }

    @Test
    fun defaultSubSystemCodesIsEmpty() {
        val f = SysTenantFormUpdate(
            id = "id-1",
            name = "n",
            timezone = null,
            defaultLanguageCode = null,
            remark = null,
        )
        assertTrue(f.subSystemCodes.isEmpty())
    }

    @Test
    fun mutableSubSystemCodes() {
        val f = form()
        f.subSystemCodes = setOf("a", "b")
        assertEquals(setOf("a", "b"), f.subSystemCodes)
    }

    @Test
    fun nullableFields() {
        val f = form().copy(timezone = null, defaultLanguageCode = null, remark = null)
        assertNull(f.timezone)
        assertNull(f.defaultLanguageCode)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "x"))
        assertTrue(a.toString().contains("Tenant One"))
    }

}
