package io.kudos.ms.sys.common.param.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysParamFormCreate
 *
 * Covers property accessors (ISysParamFormBase overrides), nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamFormCreateTest {

    private fun form() = SysParamFormCreate(
        paramName = "max.size",
        paramValue = "100",
        defaultValue = "50",
        atomicServiceCode = "sys",
        orderNum = 1,
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysParamFormBase)
        assertEquals("max.size", f.paramName)
        assertEquals("100", f.paramValue)
        assertEquals("50", f.defaultValue)
        assertEquals("sys", f.atomicServiceCode)
        assertEquals(1, f.orderNum)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableFields() {
        val f = form().copy(defaultValue = null, orderNum = null, remark = null)
        assertNull(f.defaultValue)
        assertNull(f.orderNum)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(paramValue = "200"))
        assertTrue(a.toString().contains("max.size"))
    }

}
