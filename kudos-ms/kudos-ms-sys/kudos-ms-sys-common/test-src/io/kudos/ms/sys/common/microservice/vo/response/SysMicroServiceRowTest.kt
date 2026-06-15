package io.kudos.ms.sys.common.microservice.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysMicroServiceRow
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceRowTest {

    @Test
    fun defaults() {
        val r = SysMicroServiceRow()
        assertEquals("", r.id)
        assertEquals("", r.code)
        assertEquals("", r.name)
        assertEquals("", r.context)
        assertTrue(r.atomicService)
        assertNull(r.parentCode)
        assertNull(r.remark)
        assertTrue(r.active)
        assertTrue(r.builtIn)
    }

    @Test
    fun fullArgs() {
        val r = SysMicroServiceRow(
            id = "ms_a", code = "ms_a", name = "Service A", context = "/svc-a",
            atomicService = false, parentCode = "parent", remark = "r",
            active = false, builtIn = false,
        )
        assertEquals("ms_a", r.id)
        assertEquals(false, r.atomicService)
        assertEquals(false, r.active)
        assertEquals(false, r.builtIn)
    }

    @Test
    fun dataClassContract() {
        val a = SysMicroServiceRow(id = "ms_a", code = "ms_a")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "ms_b"))
        assertTrue(a.toString().contains("ms_a"))
    }

}
