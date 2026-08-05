package io.kudos.ms.msg.core.receiver.service

import io.kudos.ms.msg.common.receiver.enums.MsgUnreceivedReasonEnum
import io.kudos.ms.msg.core.receiver.dao.MsgUnreceivedDao
import io.kudos.ms.msg.core.receiver.model.po.MsgUnreceived
import io.kudos.ms.msg.core.receiver.service.impl.MsgUnreceivedService
import io.kudos.ms.msg.core.support.MockitoKt.anyNN
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [MsgUnreceivedService] — DAO mocked, no Spring container / DB.
 *
 * Covered:
 * - recordFailures: empty receivers → returns 0 and inserts nothing; otherwise inserts one row per
 *   receiver (retryCount=0, resolved=false, reason.code stamped) and returns the receiver count
 * - resolve: writes resolved=true + updateTime and returns the update boolean
 * - bumpRetry: returns false when the record is missing; otherwise increments retryCount and stamps
 *   lastRetryTime/updateTime, returning the update boolean
 *
 * The slow integration counterpart ([MsgUnreceivedServiceTest], RdbTestBase) verifies the same
 * branches against real H2; this version keeps the pure decision logic fast.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgUnreceivedServicePureTest {

    private val dao = mock(MsgUnreceivedDao::class.java)
    private val service = MsgUnreceivedService(dao)

    @Test
    fun recordFailures_emptyReceiversIsNoOp() {
        val count = service.recordFailures(
            sendId = "s-1",
            receiverIds = emptyList(),
            publishMethodDictCode = "201",
            reason = MsgUnreceivedReasonEnum.NO_CONTACT,
            tenantId = "t-1",
        )
        assertEquals(0, count)
        verify(dao, never()).insert(anyNN(MsgUnreceived::class.java))
    }

    @Test
    fun recordFailures_insertsOneRowPerReceiverAndReturnsCount() {
        val count = service.recordFailures(
            sendId = "s-1",
            receiverIds = listOf("u-1", "u-2", "u-3"),
            publishMethodDictCode = "201",
            reason = MsgUnreceivedReasonEnum.CHANNEL_REJECT,
            tenantId = "t-1",
        )
        assertEquals(3, count)

        val captor = ArgumentCaptor.forClass(MsgUnreceived::class.java)
        verify(dao, times(3)).insert(captor.capture() ?: MsgUnreceived())
        val rows = captor.allValues
        assertEquals(listOf("u-1", "u-2", "u-3"), rows.map { it.receiverId })
        assertTrue(rows.all { it.sendId == "s-1" })
        assertTrue(rows.all { it.publishMethodDictCode == "201" })
        assertTrue(rows.all { it.failReason == MsgUnreceivedReasonEnum.CHANNEL_REJECT.code })
        assertTrue(rows.all { it.retryCount == 0 })
        assertTrue(rows.all { !it.resolved })
        assertTrue(rows.all { it.lastRetryTime == null })
        assertTrue(rows.all { it.tenantId == "t-1" })
    }

    @Test
    fun resolve_writesResolvedTrueAndReturnsResult() {
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)).thenReturn(true)

        assertTrue(service.resolve("x-1"))

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, *>>
        verify(dao).updateProperties(anyString(), captor.capture() ?: emptyMap<String, Any>())
        val props = captor.value
        assertEquals(true, props[MsgUnreceived::resolved.name])
        assertTrue(props.containsKey(MsgUnreceived::updateTime.name))
    }

    @Test
    fun bumpRetry_returnsFalseWhenMissing() {
        whenCalled(dao.get(anyString())).thenReturn(null)
        assertFalse(service.bumpRetry("missing"))
        @Suppress("UNCHECKED_CAST")
        verify(dao, never()).updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)
    }

    @Test
    fun bumpRetry_incrementsRetryCountAndStampsTime() {
        whenCalled(dao.get(anyString())).thenReturn(MsgUnreceived().apply {
            id = "x-1"
            retryCount = 2
        })
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)).thenReturn(true)

        assertTrue(service.bumpRetry("x-1"))

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, *>>
        verify(dao).updateProperties(anyString(), captor.capture() ?: emptyMap<String, Any>())
        val props = captor.value
        assertEquals(3, props[MsgUnreceived::retryCount.name], "2 + 1 = 3")
        assertTrue(props.containsKey(MsgUnreceived::lastRetryTime.name))
        assertTrue(props.containsKey(MsgUnreceived::updateTime.name))
    }

    @Test
    fun bumpRetry_propagatesUpdateFalse() {
        whenCalled(dao.get(anyString())).thenReturn(MsgUnreceived().apply {
            id = "x-1"
            retryCount = 0
        })
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)).thenReturn(false)
        assertFalse(service.bumpRetry("x-1"))
    }
}
