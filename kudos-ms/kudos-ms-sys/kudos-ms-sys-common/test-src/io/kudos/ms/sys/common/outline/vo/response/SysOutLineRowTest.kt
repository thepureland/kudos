package io.kudos.ms.sys.common.outline.vo.response

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysOutLineRow
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysOutLineRowTest {

    @Test
    fun defaults() {
        val r = SysOutLineRow()
        assertEquals("", r.id)
        assertEquals("", r.name)
        assertEquals("", r.host)
        assertNull(r.port)
        assertEquals("", r.protocol)
        assertEquals("", r.systemCode)
        assertNull(r.tenantId)
        assertNull(r.remark)
        assertTrue(r.active)
        assertEquals(false, r.builtIn)
        assertNull(r.createTime)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val r = SysOutLineRow(
            id = "o-1", name = "wl", host = "example.com", port = 443, protocol = "https",
            systemCode = "sys", tenantId = "t-1", remark = "r", active = false, builtIn = true,
            createTime = now,
        )
        assertEquals("o-1", r.id)
        assertEquals(443, r.port)
        assertEquals(false, r.active)
        assertEquals(now, r.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysOutLineRow(id = "o-1", name = "wl")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("o-1"))
    }

}
