package io.kudos.ms.msg.core.send.channel.sms

import io.kudos.ability.comm.sms.aws.handler.AwsSmsHandler
import io.kudos.ability.comm.sms.aws.model.AwsSmsCallBackParam
import io.kudos.ability.comm.sms.aws.model.AwsSmsRequest
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.enums.MsgSendStatusEnum
import io.kudos.ms.msg.common.send.vo.MsgDispatchEvent
import io.kudos.ms.msg.core.receiver.model.po.MsgReceive
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import io.kudos.ms.msg.core.support.MockitoKt.anyNN
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyCollection
import io.kudos.ms.msg.core.support.MockitoKt.eqNN
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for [MsgSmsDispatchListener] — SMS fan-out + countdown aggregation.
 *
 * [AwsSmsHandler] is mocked; one callback is captured per recipient and invoked in turn so the
 * countdown (onComplete fires exactly once on the last callback) and statusCode -> success/fail
 * classification are exercised deterministically.
 *
 * Covered:
 * - publishMethod / contactWayDictCode wiring (SMS / "101")
 * - AwsSmsRequest carries body + credentials + per-recipient phone (no subject)
 * - one send per contactable recipient
 * - statusCode in 200..299 -> success, otherwise fail; onComplete only after last callback
 * - boundary status codes 200 / 299 (success) and 199 / 300 / 500 / 599 (fail)
 * - single recipient completes on first callback
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgSmsDispatchListenerTest {

    private val smsHandler = mock(AwsSmsHandler::class.java)
    private val contactService = mock(IUserContactWayService::class.java)
    private val sendService = mock(IMsgSendService::class.java)
    private val receiveService = mock(IMsgReceiveService::class.java)
    private val unreceivedService = mock(IMsgUnreceivedService::class.java)

    private val properties = MsgSmsProperties(
        region = "ap-southeast-1",
        accessKeyId = "AKIA-TEST",
        accessKeySecret = "secret",
    )

    private fun listener() = MsgSmsDispatchListener(
        properties, smsHandler, contactService, sendService, receiveService, unreceivedService,
    )

    private fun event(receiverIds: Set<String>) = MsgDispatchEvent(
        sendId = "send-1",
        instanceId = "inst-1",
        publishMethodDictCode = "sms",
        receiverIds = receiverIds,
        renderedTitle = "Subject",
        renderedContent = "SMS body",
        localeDictCode = null,
        tenantId = "t1",
    )

    private fun vo(body: Serializable): NotifyMessageVo<Serializable> {
        val v = NotifyMessageVo<Serializable>()
        v.messageBody = body
        return v
    }

    private fun cb(code: Int) = AwsSmsCallBackParam().apply { statusCode = code }

    @Suppress("UNCHECKED_CAST")
    private fun smsCbCaptor(): ArgumentCaptor<(AwsSmsCallBackParam) -> Unit> =
        ArgumentCaptor.forClass(Function1::class.java) as ArgumentCaptor<(AwsSmsCallBackParam) -> Unit>

    @Test
    fun wiring_publishMethodAndContactWay() {
        assertEquals(MsgPublishMethodEnum.SMS.listenerType, listener().notifyType())
    }

    @Test
    fun doDispatch_fansOutOnePerRecipientAndBuildsRequest() {
        val l = listener()
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("101")))
            .thenReturn(linkedMapOf("u1" to "+1001", "u2" to "+1002"))
        whenCalled(receiveService.insert(anyNN(MsgReceive::class.java))).thenReturn("r")

        val reqCaptor = ArgumentCaptor.forClass(AwsSmsRequest::class.java)
        val cbCaptor = smsCbCaptor()

        l.notifyProcess(vo(event(setOf("u1", "u2"))))

        verify(smsHandler, times(2)).send(reqCaptor.capture() ?: AwsSmsRequest(), cbCaptor.capture() ?: { _ -> })
        val reqs = reqCaptor.allValues
        assertEquals(setOf("+1001", "+1002"), reqs.map { it.phoneNumber }.toSet())
        reqs.forEach {
            assertEquals("SMS body", it.message)
            assertEquals("ap-southeast-1", it.region)
            assertEquals("AKIA-TEST", it.accessKeyId)
        }

        // first callback: not yet complete; second: complete with both success
        val cbs = cbCaptor.allValues
        cbs[0].invoke(cb(200))
        cbs[1].invoke(cb(299))
        verify(sendService).finishSend("send-1", 2, 0, MsgSendStatusEnum.SUCCESS.dictCode)
    }

    @Test
    fun doDispatch_mixedStatusCodesClassified() {
        val l = listener()
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("101")))
            .thenReturn(linkedMapOf("u1" to "+1", "u2" to "+2", "u3" to "+3", "u4" to "+4"))
        whenCalled(receiveService.insert(anyNN(MsgReceive::class.java))).thenReturn("r")

        val cbCaptor = smsCbCaptor()
        l.notifyProcess(vo(event(setOf("u1", "u2", "u3", "u4"))))
        verify(smsHandler, times(4)).send(anyNN(AwsSmsRequest::class.java), cbCaptor.capture() ?: { _ -> })
        val cbs = cbCaptor.allValues
        // 200 success, 299 success, 199 fail, 300 fail
        cbs[0].invoke(cb(200))
        cbs[1].invoke(cb(299))
        cbs[2].invoke(cb(199))
        cbs[3].invoke(cb(300))
        verify(sendService).finishSend("send-1", 2, 2, MsgSendStatusEnum.SUCCESS_PARTIAL.dictCode)
    }

    @Test
    fun doDispatch_allFailStatusCodes_setsFailedFinal() {
        val l = listener()
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("101")))
            .thenReturn(linkedMapOf("u1" to "+1", "u2" to "+2"))

        val cbCaptor = smsCbCaptor()
        l.notifyProcess(vo(event(setOf("u1", "u2"))))
        verify(smsHandler, times(2)).send(anyNN(AwsSmsRequest::class.java), cbCaptor.capture() ?: { _ -> })
        cbCaptor.allValues[0].invoke(cb(500))
        cbCaptor.allValues[1].invoke(cb(599))
        verify(sendService).finishSend("send-1", 0, 2, MsgSendStatusEnum.FAILED_FINAL.dictCode)
    }

    @Test
    fun doDispatch_singleRecipientCompletesOnFirstCallback() {
        val l = listener()
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("101")))
            .thenReturn(linkedMapOf("u1" to "+1"))
        whenCalled(receiveService.insert(anyNN(MsgReceive::class.java))).thenReturn("r")

        val cbCaptor = smsCbCaptor()
        l.notifyProcess(vo(event(setOf("u1"))))
        verify(smsHandler, times(1)).send(anyNN(AwsSmsRequest::class.java), cbCaptor.capture() ?: { _ -> })
        cbCaptor.value.invoke(cb(200))
        verify(sendService).finishSend("send-1", 1, 0, MsgSendStatusEnum.SUCCESS.dictCode)
    }
}
