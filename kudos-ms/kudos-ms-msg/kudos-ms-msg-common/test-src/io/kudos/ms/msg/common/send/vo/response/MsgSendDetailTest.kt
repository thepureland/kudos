package io.kudos.ms.msg.common.send.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgSendDetail
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendDetailTest {

    @Test
    fun defaults() {
        val d = MsgSendDetail()
        assertEquals("", d.id)
        assertNull(d.receiverGroupId)
        assertNull(d.successCount)
        assertNull(d.failCount)
        assertNull(d.tenantId)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val ct = LocalDateTime.of(2026, 1, 1, 0, 0)
        val d = MsgSendDetail("s-1", "dept", "g-1", "i-1", "m", "en", "33", ct, null, 9, 1, "job", "tn")
        assertEquals("s-1", d.id)
        assertEquals("g-1", d.receiverGroupId)
        assertEquals(9, d.successCount)
        assertEquals(ct, d.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = MsgSendDetail(id = "s-1", instanceId = "i-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "s-2"))
        assertTrue(a.toString().contains("s-1"))
    }

}
