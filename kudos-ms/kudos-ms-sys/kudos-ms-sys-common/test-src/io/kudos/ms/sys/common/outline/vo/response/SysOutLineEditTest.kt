package io.kudos.ms.sys.common.outline.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysOutLineEdit
 *
 * Covers default values, full-args construction, IIdEntity contract and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysOutLineEditTest {

    @Test
    fun defaults() {
        val e = SysOutLineEdit()
        assertEquals("", e.id)
        assertEquals("", e.name)
        assertEquals("", e.host)
        assertNull(e.port)
        assertEquals("", e.protocol)
        assertEquals("", e.systemCode)
        assertNull(e.tenantId)
        assertNull(e.remark)
        assertTrue(e.active)
        assertEquals(false, e.builtIn)
        assertTrue(e is IIdEntity<*>)
    }

    @Test
    fun fullArgs() {
        val now = LocalDateTime.of(2026, 6, 15, 10, 0)
        val e = SysOutLineEdit(
            id = "o-1", name = "wl", host = "example.com", port = 443, protocol = "https",
            systemCode = "sys", tenantId = "t-1", remark = "r", active = false, builtIn = true,
            createTime = now, updateTime = now,
        )
        assertEquals("o-1", e.id)
        assertEquals(443, e.port)
        assertEquals(false, e.active)
        assertEquals(now, e.updateTime)
    }

    @Test
    fun dataClassContract() {
        val a = SysOutLineEdit(id = "o-1", name = "wl")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("o-1"))
    }

}
