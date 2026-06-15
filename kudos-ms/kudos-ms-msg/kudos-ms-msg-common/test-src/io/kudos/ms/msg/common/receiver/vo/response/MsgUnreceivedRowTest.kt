package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgUnreceivedRow
 *
 * Covers defaults, full construction (incl. Int retryCount and Boolean resolved),
 * IIdEntity contract and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgUnreceivedRowTest {

    @Test
    fun defaults() {
        val r = MsgUnreceivedRow()
        assertEquals("", r.id)
        assertNull(r.receiverId)
        assertNull(r.sendId)
        assertNull(r.publishMethodDictCode)
        assertNull(r.failReason)
        assertNull(r.retryCount)
        assertNull(r.lastRetryTime)
        assertNull(r.resolved)
        assertNull(r.createTime)
        assertNull(r.updateTime)
        assertNull(r.tenantId)
        assertTrue(r is IIdEntity<*>)
    }

    @Test
    fun fullConstruction() {
        val lr = LocalDateTime.of(2026, 1, 1, 0, 0)
        val r = MsgUnreceivedRow(
            id = "x-1", receiverId = "u-1", sendId = "s-1", publishMethodDictCode = "email",
            failReason = "NO_CONTACT", retryCount = 3, lastRetryTime = lr, resolved = false,
            createTime = null, updateTime = null, tenantId = "tn",
        )
        assertEquals("x-1", r.id)
        assertEquals("email", r.publishMethodDictCode)
        assertEquals("NO_CONTACT", r.failReason)
        assertEquals(3, r.retryCount)
        assertEquals(lr, r.lastRetryTime)
        assertEquals(false, r.resolved)
        assertEquals("tn", r.tenantId)
    }

    @Test
    fun dataClassContract() {
        val a = MsgUnreceivedRow(id = "x-1", retryCount = 1)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(retryCount = 2))
        assertTrue(a.toString().contains("x-1"))
    }

}
