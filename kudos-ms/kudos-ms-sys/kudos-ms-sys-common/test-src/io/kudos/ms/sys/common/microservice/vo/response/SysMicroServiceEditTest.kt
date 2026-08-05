package io.kudos.ms.sys.common.microservice.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysMicroServiceEdit
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceEditTest {

    @Test
    fun defaults() {
        val e = SysMicroServiceEdit()
        assertEquals("", e.id)
        assertEquals("", e.code)
        assertEquals("", e.name)
        assertEquals("", e.context)
        assertTrue(e.atomicService)
        assertNull(e.parentCode)
        assertNull(e.remark)
        assertTrue(e.active)
        assertTrue(e.builtIn)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysMicroServiceEdit(
            id = "ms_a", code = "ms_a", name = "Service A", context = "/svc-a",
            atomicService = false, parentCode = "parent", remark = "r",
            active = false, builtIn = false, createTime = now, updateTime = now,
        )
        assertEquals("ms_a", e.id)
        assertEquals(false, e.atomicService)
        assertEquals(false, e.builtIn)
        assertEquals(now, e.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysMicroServiceEdit(id = "ms_a", code = "ms_a")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "ms_b"))
        assertTrue(a.toString().contains("ms_a"))
    }

}
