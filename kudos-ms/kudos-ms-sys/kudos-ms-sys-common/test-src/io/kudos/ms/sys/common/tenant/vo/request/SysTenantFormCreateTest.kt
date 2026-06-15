package io.kudos.ms.sys.common.tenant.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysTenantFormCreate
 *
 * Covers ISysTenantFormBase property accessors (incl. mutable subSystemCodes and
 * nullable timezone / defaultLanguageCode / remark) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantFormCreateTest {

    private fun form() = SysTenantFormCreate(
        name = "Tenant One",
        subSystemCodes = setOf("sys", "crm"),
        timezone = "Asia/Shanghai",
        defaultLanguageCode = "zh-CN",
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysTenantFormBase)
        assertEquals("Tenant One", f.name)
        assertEquals(setOf("sys", "crm"), f.subSystemCodes)
        assertEquals("Asia/Shanghai", f.timezone)
        assertEquals("zh-CN", f.defaultLanguageCode)
        assertEquals("r", f.remark)
    }

    @Test
    fun mutableSubSystemCodes() {
        val f = form()
        f.subSystemCodes = setOf("only")
        assertEquals(setOf("only"), f.subSystemCodes)
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
        assertNotEquals(a, a.copy(name = "Other"))
        assertTrue(a.toString().contains("Tenant One"))
    }

}
