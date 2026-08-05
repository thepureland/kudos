package io.kudos.ms.sys.common.microservice.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysMicroServiceDetail
 *
 * Covers default values (atomicService / active / builtIn all default true), full-args
 * construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMicroServiceDetailTest {

    @Test
    fun defaults() {
        val d = SysMicroServiceDetail()
        assertEquals("", d.id)
        assertEquals("", d.code)
        assertEquals("", d.name)
        assertEquals("", d.context)
        assertTrue(d.atomicService)
        assertNull(d.parentCode)
        assertNull(d.remark)
        assertTrue(d.active)
        assertTrue(d.builtIn)
        assertNull(d.createTime)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysMicroServiceDetail(
            id = "ms_a", code = "ms_a", name = "Service A", context = "/svc-a",
            atomicService = false, parentCode = "parent", remark = "r",
            active = false, builtIn = false, createTime = now, updateTime = now,
        )
        assertEquals("ms_a", d.id)
        assertEquals(false, d.atomicService)
        assertEquals(false, d.active)
        assertEquals(false, d.builtIn)
        assertEquals(now, d.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysMicroServiceDetail(id = "ms_a", code = "ms_a")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(code = "ms_b"))
        assertTrue(a.toString().contains("ms_a"))
    }

}
