package io.kudos.ms.sys.common.param.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysParamFormUpdate
 *
 * Covers property accessors (id + ISysParamFormBase overrides), IIdEntity contract,
 * nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamFormUpdateTest {

    private fun form() = SysParamFormUpdate(
        id = "p-1",
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
        assertTrue(f is IIdEntity<*>)
        assertEquals("p-1", f.id)
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
        assertNotEquals(a, a.copy(id = "p-2"))
        assertTrue(a.toString().contains("p-1"))
    }

}
