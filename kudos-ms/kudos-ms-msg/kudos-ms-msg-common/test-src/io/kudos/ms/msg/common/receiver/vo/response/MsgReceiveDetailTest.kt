package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiveDetail
 *
 * Covers defaults, full construction, IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveDetailTest {

    @Test
    fun defaults() {
        val d = MsgReceiveDetail()
        assertEquals("", d.id)
        assertNull(d.receiverId)
        assertNull(d.sendId)
        assertNull(d.createTime)
        assertNull(d.tenantId)
        assertTrue(d is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val ct = LocalDateTime.of(2026, 1, 1, 0, 0)
        val d = MsgReceiveDetail("r-1", "u-1", "s-1", "11", ct, null, "tn")
        assertEquals("r-1", d.id)
        assertEquals("u-1", d.receiverId)
        assertEquals(ct, d.createTime)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiveDetail(id = "r-1", receiverId = "u-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "r-2"))
        assertTrue(a.toString().contains("r-1"))
    }

}
