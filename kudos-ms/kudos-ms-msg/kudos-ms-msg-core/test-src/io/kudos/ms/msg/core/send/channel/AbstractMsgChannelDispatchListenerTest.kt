package io.kudos.ms.msg.core.send.channel

import com.alibaba.fastjson2.JSON
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ms.msg.common.receiver.enums.MsgUnreceivedReasonEnum
import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.enums.MsgSendStatusEnum
import io.kudos.ms.msg.common.send.vo.MsgDispatchEvent
import io.kudos.ms.msg.core.receiver.model.po.MsgReceive
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import io.kudos.ms.msg.core.support.MockitoKt.anyNN
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyString
import io.kudos.ms.msg.core.support.MockitoKt.eqNN
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure unit test for [AbstractMsgChannelDispatchListener] — the template-method dispatch skeleton.
 *
 * Uses a minimal in-test concrete subclass ([TestListener]) whose [doDispatch] reports a
 * caller-controlled success/fail split synchronously, so every branch of the base flow can be
 * driven without MQ / DB / a real channel.
 *
 * Covered:
 * - decodePayload: null body, already-typed event, JSONObject round-trip, undeserializable garbage
 * - notifyProcess: status -> CONSUMED_FROM_MQ; empty receivers -> FAILED_FINAL
 * - no-contact subset recorded as NO_CONTACT failures (counted into failCount)
 * - all receivers without contact -> FAILED_FINAL, doDispatch not invoked
 * - finalizeDispatch aggregation: all success -> SUCCESS; none -> FAILED_FINAL; mixed -> SUCCESS_PARTIAL
 * - successful MsgReceive insert; insert failure swallowed by runCatching (batch continues)
 *
 * @author K
 * @since 1.0.0
 */
internal class AbstractMsgChannelDispatchListenerTest {

    private val contactService = mock(IUserContactWayService::class.java)
    private val sendService = mock(IMsgSendService::class.java)
    private val receiveService = mock(IMsgReceiveService::class.java)
    private val unreceivedService = mock(IMsgUnreceivedService::class.java)

    /** Concrete subclass: doDispatch invokes onComplete with the configured split (filtered to contactable users). */
    private inner class TestListener(
        private val split: (Map<String, String>) -> Pair<Collection<String>, Collection<String>>,
    ) : AbstractMsgChannelDispatchListener(contactService, sendService, receiveService, unreceivedService) {
        override val publishMethod: MsgPublishMethodEnum = MsgPublishMethodEnum.EMAIL
        override val contactWayDictCode: String = "201"
        var dispatchInvoked = false
        override fun doDispatch(
            event: MsgDispatchEvent,
            contactByUserId: Map<String, String>,
            onComplete: (successUserIds: Collection<String>, failUserIds: Collection<String>) -> Unit,
        ) {
            dispatchInvoked = true
            val (ok, fail) = split(contactByUserId)
            onComplete(ok, fail)
        }
    }

    private fun event(receiverIds: Set<String>) = MsgDispatchEvent(
        sendId = "send-1",
        instanceId = "inst-1",
        publishMethodDictCode = "email",
        receiverIds = receiverIds,
        renderedTitle = "Hi",
        renderedContent = "Body",
        localeDictCode = null,
        tenantId = "t1",
    )

    private fun vo(body: Serializable?): NotifyMessageVo<Serializable> {
        val v = NotifyMessageVo<Serializable>()
        v.messageBody = body
        return v
    }

    private fun listener(split: (Map<String, String>) -> Pair<Collection<String>, Collection<String>> = { m ->
        m.keys.toList() to emptyList()
    }) = TestListener(split)

    // ---------- notifyType ----------

    @Test
    fun notifyType_matchesPublishMethodListenerType() {
        assertEquals(MsgPublishMethodEnum.EMAIL.listenerType, listener().notifyType())
    }

    // ---------- decodePayload ----------

    @Test
    fun decodePayload_nullBodyReturnsNull() {
        val l = listener()
        val m = AbstractMsgChannelDispatchListener::class.java
            .getDeclaredMethod("decodePayload", Serializable::class.java)
        m.isAccessible = true
        assertNull(m.invoke(l, *arrayOf<Any?>(null)))
    }

    @Test
    fun decodePayload_typedEventReturnedAsIs() {
        val l = listener()
        val ev = event(setOf("u1"))
        val m = AbstractMsgChannelDispatchListener::class.java
            .getDeclaredMethod("decodePayload", Serializable::class.java)
        m.isAccessible = true
        val decoded = m.invoke(l, ev)
        assertSame(ev, decoded)
    }

    @Test
    fun decodePayload_jsonObjectRoundTrips() {
        val l = listener()
        val ev = event(setOf("u1", "u2"))
        val jsonObject = JSON.parseObject(JSON.toJSONString(ev)) // fastjson JSONObject (Serializable)
        val m = AbstractMsgChannelDispatchListener::class.java
            .getDeclaredMethod("decodePayload", Serializable::class.java)
        m.isAccessible = true
        val decoded = m.invoke(l, jsonObject) as MsgDispatchEvent
        assertEquals("send-1", decoded.sendId)
        assertEquals(setOf("u1", "u2"), decoded.receiverIds)
        assertEquals("Hi", decoded.renderedTitle)
    }

    @Test
    fun decodePayload_garbageReturnsNull() {
        val l = listener()
        // A String that is not valid JSON for MsgDispatchEvent -> parse throws -> getOrElse -> null
        val m = AbstractMsgChannelDispatchListener::class.java
            .getDeclaredMethod("decodePayload", Serializable::class.java)
        m.isAccessible = true
        assertNull(m.invoke(l, "{this is not json"))
    }

    // ---------- notifyProcess flow ----------

    @Test
    fun notifyProcess_nullBodyShortCircuits() {
        val l = listener()
        l.notifyProcess(vo(null))
        verify(sendService, never()).updateSendStatus(anyString(), anyString())
        assertTrue(!l.dispatchInvoked)
    }

    @Test
    fun notifyProcess_advancesStatusToConsumed() {
        val l = listener()
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(mapOf("u1" to "u1@x.com"))
        l.notifyProcess(vo(event(setOf("u1"))))
        verify(sendService).updateSendStatus("send-1", MsgSendStatusEnum.CONSUMED_FROM_MQ.dictCode)
    }

    @Test
    fun notifyProcess_emptyReceiversFinishesFailedFinal() {
        val l = listener()
        l.notifyProcess(vo(event(emptySet())))
        verify(sendService).updateSendStatus("send-1", MsgSendStatusEnum.CONSUMED_FROM_MQ.dictCode)
        verify(sendService).finishSend("send-1", 0, 0, MsgSendStatusEnum.FAILED_FINAL.dictCode)
        assertTrue(!l.dispatchInvoked)
    }

    @Test
    fun notifyProcess_allReceiversNoContactFinishesFailedFinal() {
        val l = listener()
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(emptyMap())
        l.notifyProcess(vo(event(setOf("u1", "u2"))))
        // NO_CONTACT recorded for both
        verify(unreceivedService).recordFailures(
            eqNN("send-1"), anyCollection(), eqNN("email"), eqNN(MsgUnreceivedReasonEnum.NO_CONTACT), eqNN("t1"),
        )
        verify(sendService).finishSend("send-1", 0, 2, MsgSendStatusEnum.FAILED_FINAL.dictCode)
        assertTrue(!l.dispatchInvoked, "doDispatch must be skipped when no one is contactable")
    }

    @Test
    fun notifyProcess_partialNoContactCountedAsFailures_allChannelSuccess() {
        // u1,u2 contactable; u3 has no contact -> u3 NO_CONTACT, u1+u2 channel success
        val l = listener { m -> m.keys.toList() to emptyList() }
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(mapOf("u1" to "u1@x.com", "u2" to "u2@x.com"))
        whenCalled(receiveService.insert(anyNN(MsgReceive::class.java))).thenReturn("r")
        l.notifyProcess(vo(event(setOf("u1", "u2", "u3"))))

        verify(unreceivedService).recordFailures(
            eqNN("send-1"), anyCollection(), eqNN("email"), eqNN(MsgUnreceivedReasonEnum.NO_CONTACT), eqNN("t1"),
        )
        // success=2, fail = 0 channel + 1 no-contact = 1 -> SUCCESS_PARTIAL
        verify(sendService).finishSend("send-1", 2, 1, MsgSendStatusEnum.SUCCESS_PARTIAL.dictCode)
        verify(receiveService, times(2)).insert(anyNN(MsgReceive::class.java))
    }

    @Test
    fun finalize_allSuccessNoUnreachable_setsSuccess() {
        val l = listener { m -> m.keys.toList() to emptyList() }
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(mapOf("u1" to "u1@x.com", "u2" to "u2@x.com"))
        whenCalled(receiveService.insert(anyNN(MsgReceive::class.java))).thenReturn("r")
        l.notifyProcess(vo(event(setOf("u1", "u2"))))
        verify(sendService).finishSend("send-1", 2, 0, MsgSendStatusEnum.SUCCESS.dictCode)
        verify(receiveService, times(2)).insert(anyNN(MsgReceive::class.java))
        // no channel-reject failures recorded
        verify(unreceivedService, never()).recordFailures(
            anyString(), anyCollection(), anyString(), eqNN(MsgUnreceivedReasonEnum.CHANNEL_REJECT), anyString(),
        )
    }

    @Test
    fun finalize_allChannelFail_setsFailedFinal() {
        val l = listener { m -> emptyList<String>() to m.keys.toList() }
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(mapOf("u1" to "u1@x.com", "u2" to "u2@x.com"))
        l.notifyProcess(vo(event(setOf("u1", "u2"))))
        verify(unreceivedService).recordFailures(
            eqNN("send-1"), anyCollection(), eqNN("email"), eqNN(MsgUnreceivedReasonEnum.CHANNEL_REJECT), eqNN("t1"),
        )
        verify(sendService).finishSend("send-1", 0, 2, MsgSendStatusEnum.FAILED_FINAL.dictCode)
        verify(receiveService, never()).insert(anyNN(MsgReceive::class.java))
    }

    @Test
    fun finalize_mixedChannelOutcome_setsSuccessPartial() {
        // u1 success, u2 fail
        val l = listener { m ->
            val keys = m.keys.toList()
            listOf(keys[0]) to listOf(keys[1])
        }
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(linkedMapOf("u1" to "u1@x.com", "u2" to "u2@x.com"))
        whenCalled(receiveService.insert(anyNN(MsgReceive::class.java))).thenReturn("r")
        l.notifyProcess(vo(event(setOf("u1", "u2"))))
        verify(sendService).finishSend("send-1", 1, 1, MsgSendStatusEnum.SUCCESS_PARTIAL.dictCode)
        verify(receiveService, times(1)).insert(anyNN(MsgReceive::class.java))
    }

    @Test
    fun finalize_receiveInsertFailureIsSwallowed_batchContinues() {
        // both succeed at channel; first insert throws, second succeeds; status still SUCCESS
        val l = listener { m -> m.keys.toList() to emptyList() }
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(linkedMapOf("u1" to "u1@x.com", "u2" to "u2@x.com"))
        whenCalled(receiveService.insert(anyNN(MsgReceive::class.java)))
            .thenThrow(RuntimeException("db down"))
            .thenReturn("r")
        l.notifyProcess(vo(event(setOf("u1", "u2"))))
        // runCatching swallows; both attempted; aggregate still SUCCESS (successCount derived from list, not inserts)
        verify(receiveService, times(2)).insert(anyNN(MsgReceive::class.java))
        verify(sendService).finishSend("send-1", 2, 0, MsgSendStatusEnum.SUCCESS.dictCode)
    }
}
