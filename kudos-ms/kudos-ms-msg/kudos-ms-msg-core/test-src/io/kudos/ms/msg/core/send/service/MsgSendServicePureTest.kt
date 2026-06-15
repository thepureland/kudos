package io.kudos.ms.msg.core.send.service

import io.kudos.ms.msg.common.send.enums.MsgSendStatusEnum
import io.kudos.ms.msg.core.send.dao.MsgSendDao
import io.kudos.ms.msg.core.send.model.po.MsgSend
import io.kudos.ms.msg.core.send.service.impl.MsgSendService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit test for [MsgSendService] — DAO mocked, no Spring container / DB.
 *
 * Covered:
 * - findByIdempotencyKey: returns the first match; null when the search is empty
 * - updateSendStatus: delegates to updateProperties (status + updateTime), returns its boolean
 * - finishSend: returns false when the record is missing; otherwise accumulates counts
 *   (treating null successCount/failCount as 0) and writes final status
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSendServicePureTest {

    private val dao = mock(MsgSendDao::class.java)
    private val service = MsgSendService(dao)

    private fun send(success: Int?, fail: Int?) = MsgSend().apply {
        id = "s-1"
        successCount = success
        failCount = fail
    }

    @Test
    fun findByIdempotencyKey_returnsFirstMatch() {
        val match = MsgSend().apply { id = "found" }
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.andSearch(anyMap<Any, Any>() as Map<KProperty1<MsgSend, *>, *>))
            .thenReturn(listOf(match, MsgSend().apply { id = "other" }))
        val result = service.findByIdempotencyKey("t1", "key-1")
        assertEquals("found", result?.id)
    }

    @Test
    fun findByIdempotencyKey_returnsNullWhenEmpty() {
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.andSearch(anyMap<Any, Any>() as Map<KProperty1<MsgSend, *>, *>))
            .thenReturn(emptyList())
        assertNull(service.findByIdempotencyKey("t1", "none"))
    }

    @Test
    fun updateSendStatus_delegatesAndReturnsTrue() {
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>))
            .thenReturn(true)
        assertTrue(service.updateSendStatus("s-1", "11"))

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, *>>
        verify(dao).updateProperties(anyString(), captor.capture() ?: emptyMap<String, Any>())
        val props = captor.value
        assertEquals("11", props[MsgSend::sendStatusDictCode.name])
        assertTrue(props.containsKey(MsgSend::updateTime.name))
        // counts must not be touched by updateSendStatus
        assertFalse(props.containsKey(MsgSend::successCount.name))
        assertFalse(props.containsKey(MsgSend::failCount.name))
    }

    @Test
    fun updateSendStatus_propagatesFalse() {
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>))
            .thenReturn(false)
        assertFalse(service.updateSendStatus("s-1", "11"))
    }

    @Test
    fun finishSend_returnsFalseWhenRecordMissing() {
        whenCalled(dao.get(anyString())).thenReturn(null)
        assertFalse(service.finishSend("missing", 1, 1, MsgSendStatusEnum.SUCCESS.dictCode))
    }

    @Test
    fun finishSend_accumulatesCountsAndSetsStatus() {
        whenCalled(dao.get(anyString())).thenReturn(send(success = 2, fail = 1))
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>))
            .thenReturn(true)

        val ok = service.finishSend("s-1", successDelta = 3, failDelta = 1, finalStatusDictCode = "33")
        assertTrue(ok)

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, *>>
        verify(dao).updateProperties(anyString(), captor.capture() ?: emptyMap<String, Any>())
        val props = captor.value
        assertEquals(5, props[MsgSend::successCount.name], "2 + 3 = 5")
        assertEquals(2, props[MsgSend::failCount.name], "1 + 1 = 2")
        assertEquals("33", props[MsgSend::sendStatusDictCode.name])
        assertTrue(props.containsKey(MsgSend::updateTime.name))
    }

    @Test
    fun finishSend_treatsNullCountsAsZero() {
        whenCalled(dao.get(anyString())).thenReturn(send(success = null, fail = null))
        @Suppress("UNCHECKED_CAST")
        whenCalled(dao.updateProperties(anyString(), anyMap<Any, Any>() as Map<String, *>))
            .thenReturn(true)

        assertTrue(service.finishSend("s-1", successDelta = 4, failDelta = 2, finalStatusDictCode = "32"))

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, *>>
        verify(dao).updateProperties(anyString(), captor.capture() ?: emptyMap<String, Any>())
        assertEquals(4, captor.value[MsgSend::successCount.name], "null -> 0, 0 + 4 = 4")
        assertEquals(2, captor.value[MsgSend::failCount.name], "null -> 0, 0 + 2 = 2")
    }
}
