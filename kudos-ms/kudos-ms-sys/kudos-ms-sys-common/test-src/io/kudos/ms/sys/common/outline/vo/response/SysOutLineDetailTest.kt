package io.kudos.ms.sys.common.outline.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysOutLineDetail
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysOutLineDetailTest {

    @Test
    fun defaults() {
        val d = SysOutLineDetail()
        assertEquals("", d.id)
        assertEquals("", d.name)
        assertEquals("", d.host)
        assertNull(d.port)
        assertEquals("", d.protocol)
        assertEquals("", d.systemCode)
        assertNull(d.tenantId)
        assertNull(d.remark)
        assertTrue(d.active)
        assertEquals(false, d.builtIn)
        assertNull(d.createTime)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val d = SysOutLineDetail(
            id = "o-1", name = "wl", host = "example.com", port = 443, protocol = "https",
            systemCode = "sys", tenantId = "t-1", remark = "r", active = false, builtIn = true,
            createTime = now, updateTime = now,
        )
        assertEquals("o-1", d.id)
        assertEquals(443, d.port)
        assertEquals(false, d.active)
        assertEquals(true, d.builtIn)
        assertEquals(now, d.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysOutLineDetail(id = "o-1", name = "wl")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("o-1"))
    }

}
