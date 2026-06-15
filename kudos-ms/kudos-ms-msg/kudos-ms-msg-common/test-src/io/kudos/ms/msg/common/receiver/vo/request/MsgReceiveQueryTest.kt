package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiveRow
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiveQuery
 *
 * Covers all-null defaults, full construction, ListSearchPayload overrides and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveQueryTest {

    @Test
    fun defaultsAreAllNull() {
        val q = MsgReceiveQuery()
        assertNull(q.receiverId)
        assertNull(q.sendId)
        assertNull(q.receiveStatusDictCode)
        assertNull(q.createTime)
        assertNull(q.updateTime)
        assertNull(q.tenantId)
    }

    @Test
    fun fullArgsConstruction() {
        val ct = LocalDateTime.of(2026, 1, 1, 0, 0)
        val q = MsgReceiveQuery("u-1", "s-1", "11", ct, null, "tn")
        assertEquals("u-1", q.receiverId)
        assertEquals("s-1", q.sendId)
        assertEquals(ct, q.createTime)
        assertEquals("tn", q.tenantId)
    }

    @Test
    fun payloadOverrides() {
        val q = MsgReceiveQuery()
        assertEquals(MsgReceiveRow::class, q.getReturnEntityClass())
        assertFalse(q.isUnpagedSearchAllowed())
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiveQuery(receiverId = "u-1")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(receiverId = "u-2"))
        assertTrue(a.toString().contains("u-1"))
    }

}
