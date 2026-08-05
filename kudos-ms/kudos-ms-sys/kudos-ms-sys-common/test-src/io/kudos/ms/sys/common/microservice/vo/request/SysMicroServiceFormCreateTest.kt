package io.kudos.ms.sys.common.microservice.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysMicroServiceFormCreate
 *
 * Covers ISysMicroServiceFormBase property accessors (including nullable parentCode / remark
 * and the atomicService boolean) and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceFormCreateTest {

    private fun form() = SysMicroServiceFormCreate(
        code = "ms_a",
        name = "Service A",
        context = "/svc-a",
        atomicService = true,
        parentCode = "parent",
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysMicroServiceFormBase)
        assertEquals("ms_a", f.code)
        assertEquals("Service A", f.name)
        assertEquals("/svc-a", f.context)
        assertTrue(f.atomicService)
        assertEquals("parent", f.parentCode)
        assertEquals("r", f.remark)
    }

    @Test
    fun nullableAndFalseFields() {
        val f = form().copy(atomicService = false, parentCode = null, remark = null)
        assertFalse(f.atomicService)
        assertNull(f.parentCode)
        assertNull(f.remark)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "ms_b"))
        assertTrue(a.toString().contains("ms_a"))
    }

}
