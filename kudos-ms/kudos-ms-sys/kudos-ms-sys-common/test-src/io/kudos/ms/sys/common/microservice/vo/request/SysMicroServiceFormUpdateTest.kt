package io.kudos.ms.sys.common.microservice.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysMicroServiceFormUpdate
 *
 * Covers ISysMicroServiceFormBase accessors, the overridden id getter (= code), the
 * atomicService default (true), nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceFormUpdateTest {

    private fun form() = SysMicroServiceFormUpdate(
        code = "ms_a",
        name = "Service A",
        context = "/svc-a",
        atomicService = false,
        parentCode = "parent",
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is ISysMicroServiceFormBase)
        assertTrue(f is IIdEntity<*>)
        assertEquals("ms_a", f.code)
        assertEquals("Service A", f.name)
        assertEquals("/svc-a", f.context)
        assertFalse(f.atomicService)
        assertEquals("parent", f.parentCode)
        assertEquals("r", f.remark)
    }

    @Test
    fun idEqualsCode() {
        val f = form()
        assertEquals("ms_a", f.id)
        assertEquals(f.code, f.id)
    }

    @Test
    fun atomicServiceDefaultsToTrue() {
        val f = SysMicroServiceFormUpdate(
            code = "ms_a",
            name = "Service A",
            context = "/svc-a",
            parentCode = null,
            remark = null,
        )
        assertTrue(f.atomicService)
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
