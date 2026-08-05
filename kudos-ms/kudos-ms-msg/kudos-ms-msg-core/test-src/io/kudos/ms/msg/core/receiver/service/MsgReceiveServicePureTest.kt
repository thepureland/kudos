package io.kudos.ms.msg.core.receiver.service

import io.kudos.base.query.Criteria
import io.kudos.ms.msg.common.receiver.enums.MsgReceiveStatusEnum
import io.kudos.ms.msg.core.receiver.dao.MsgReceiveDao
import io.kudos.ms.msg.core.receiver.model.po.MsgReceive
import io.kudos.ms.msg.core.receiver.service.impl.MsgReceiveService
import io.kudos.ms.msg.core.support.MockitoKt.anyNN
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [MsgReceiveService] — DAO mocked, no Spring container / DB.
 *
 * Covered:
 * - getUnreadCountByUserId: delegates [MsgReceiveDao.count] and returns its value
 * - markRead: returns false when the record is missing; returns false (and never writes) when the
 *   record is already READ/DELETED (non-unread); otherwise flips status to READ and returns the
 *   underlying update boolean
 * - markAllReadByUserId: delegates [MsgReceiveDao.batchUpdateProperties] with READ status and returns
 *   the affected-row count
 *
 * The slow integration counterpart ([MsgReceiveServiceTest], RdbTestBase) verifies the same
 * branches against real H2; this version keeps the pure decision logic fast.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiveServicePureTest {

    private val dao = mock(MsgReceiveDao::class.java)
    private val service = MsgReceiveService(dao)

    private fun receive(status: String) = MsgReceive().apply {
        id = "r-1"
        receiverId = "u-1"
        sendId = "s-1"
        receiveStatusDictCode = status
    }

    @Test
    fun getUnreadCountByUserId_delegatesToCount() {
        whenCalled(dao.count(any(Criteria::class.java))).thenReturn(7)
        assertEquals(7, service.getUnreadCountByUserId("u-1"))
    }

    @Test
    fun markRead_returnsFalseWhenRecordMissing() {
        whenCalled(dao.get(anyString())).thenReturn(null)
        assertFalse(service.markRead("missing"))
        @Suppress("UNCHECKED_CAST")
        verify(dao, never()).updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)
    }

    @Test
    fun markRead_returnsFalseWhenAlreadyRead() {
        whenCalled(dao.get(anyString())).thenReturn(receive(MsgReceiveStatusEnum.READ.dictCode))
        assertFalse(service.markRead("r-1"))
        @Suppress("UNCHECKED_CAST")
        verify(dao, never()).updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)
    }

    @Test
    fun markRead_returnsFalseWhenDeleted() {
        whenCalled(dao.get(anyString())).thenReturn(receive(MsgReceiveStatusEnum.DELETED.dictCode))
        assertFalse(service.markRead("r-1"))
        @Suppress("UNCHECKED_CAST")
        verify(dao, never()).updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)
    }

    @Test
    fun markRead_flipsUnreadToReadAndReturnsUpdateResult() {
        whenCalled(dao.get(anyString())).thenReturn(receive(MsgReceiveStatusEnum.UNREAD.dictCode))
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)).thenReturn(true)

        assertTrue(service.markRead("r-1"))

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, *>>
        verify(dao).updateProperties(anyString(), captor.capture() ?: emptyMap<String, Any>())
        val props = captor.value
        assertEquals(MsgReceiveStatusEnum.READ.dictCode, props[MsgReceive::receiveStatusDictCode.name])
        assertTrue(props.containsKey(MsgReceive::updateTime.name))
    }

    @Test
    fun markRead_treatsReceivedAsUnreadAndMarksRead() {
        // RECEIVED ("11") is part of UNREAD_CODES, so it should be markable.
        whenCalled(dao.get(anyString())).thenReturn(receive(MsgReceiveStatusEnum.RECEIVED.dictCode))
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)).thenReturn(true)
        assertTrue(service.markRead("r-1"))
    }

    @Test
    fun markRead_propagatesUpdateFalse() {
        whenCalled(dao.get(anyString())).thenReturn(receive(MsgReceiveStatusEnum.UNREAD.dictCode))
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>)).thenReturn(false)
        assertFalse(service.markRead("r-1"))
    }

    @Test
    fun markAllReadByUserId_delegatesBatchUpdateAndReturnsCount() {
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.batchUpdateProperties(anyNN(Criteria::class.java), anyMap<Any, Any>() as Map<String, *>))
            .thenReturn(5)

        assertEquals(5, service.markAllReadByUserId("u-1"))

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, *>>
        verify(dao).batchUpdateProperties(anyNN(Criteria::class.java), captor.capture() ?: emptyMap<String, Any>())
        val props = captor.value
        assertEquals(MsgReceiveStatusEnum.READ.dictCode, props[MsgReceive::receiveStatusDictCode.name])
        assertTrue(props.containsKey(MsgReceive::updateTime.name))
    }
}
