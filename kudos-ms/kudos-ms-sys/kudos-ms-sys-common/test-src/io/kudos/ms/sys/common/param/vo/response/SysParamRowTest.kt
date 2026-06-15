package io.kudos.ms.sys.common.param.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysParamRow
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamRowTest {

    @Test
    fun defaults() {
        val r = SysParamRow()
        assertEquals("", r.id)
        assertEquals("", r.paramName)
        assertEquals("", r.paramValue)
        assertNull(r.defaultValue)
        assertEquals("", r.atomicServiceCode)
        assertNull(r.orderNum)
        assertNull(r.remark)
        assertEquals(true, r.active)
        assertEquals(true, r.builtIn)
    }

    @Test
    fun fullArgs() {
        val r = SysParamRow(
            id = "p-1",
            paramName = "max.size",
            paramValue = "100",
            defaultValue = "50",
            atomicServiceCode = "sys",
            orderNum = 1,
            remark = "r",
            active = false,
            builtIn = false,
        )
        assertEquals("p-1", r.id)
        assertEquals(1, r.orderNum)
        assertEquals(false, r.active)
    }

    @Test
    fun dataClassContract() {
        val a = SysParamRow(id = "p-1", paramName = "max.size")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("p-1"))
    }

}
