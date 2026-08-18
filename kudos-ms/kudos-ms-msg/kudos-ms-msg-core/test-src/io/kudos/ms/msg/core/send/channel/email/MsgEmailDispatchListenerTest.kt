package io.kudos.ms.msg.core.send.channel.email

import io.kudos.ability.comm.email.handler.EmailHandler
import io.kudos.ability.comm.email.model.EmailCallBackParam
import io.kudos.ability.comm.email.model.EmailRequest
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.enums.MsgSendStatusEnum
import io.kudos.ms.msg.common.send.vo.MsgDispatchEvent
import io.kudos.ms.msg.core.receiver.model.po.MsgReceive
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import io.kudos.ms.msg.core.support.MockitoKt.anyNN
import io.kudos.ms.user.client.contact.proxy.IUserContactWayProxy
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
 * Pure unit test for [MsgEmailDispatchListener] — email channel specifics on top of the base flow.
 *
 * [EmailHandler] is mocked; its async send callback is captured and invoked synchronously so the
 * success/fail email -> userId mapping and downstream aggregation are exercised deterministically.
 *
 * Covered:
 * - publishMethod / contactWayDictCode wiring (EMAIL / "201")
 * - EmailRequest assembled from MsgEmailProperties + rendered title/body, deduped recipient set
 * - callback success/fail email lists mapped back to userIds (incl. a shared address -> multiple users)
 * - aggregation through the base: all success -> SUCCESS; mixed -> SUCCESS_PARTIAL; null lists -> empty
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgEmailDispatchListenerTest {

    private val emailHandler = mock(EmailHandler::class.java)
    private val contactService = mock(IUserContactWayProxy::class.java)
    private val sendService = mock(IMsgSendService::class.java)
    private val receiveService = mock(IMsgReceiveService::class.java)
    private val unreceivedService = mock(IMsgUnreceivedService::class.java)

    private val properties = MsgEmailProperties(
        serverHost = "smtp.test",
        serverPort = 465,
        senderAccount = "notify@test",
        senderPassword = "pwd",
        fromMailAddress = "from@test",
        ssl = true,
    )

    private fun listener() = MsgEmailDispatchListener(
        properties, emailHandler, contactService, sendService, receiveService, unreceivedService,
    )

    private fun event(receiverIds: Set<String>) = MsgDispatchEvent(
        sendId = "send-1",
        instanceId = "inst-1",
        publishMethodDictCode = "email",
        receiverIds = receiverIds,
        renderedTitle = "Subject",
        renderedContent = "Body",
        localeDictCode = null,
        tenantId = "t1",
    )

    private fun vo(body: Serializable): NotifyMessageVo<Serializable> {
        val v = NotifyMessageVo<Serializable>()
        v.messageBody = body
        return v
    }

    @Suppress("UNCHECKED_CAST")
    private fun emailCbCaptor(): ArgumentCaptor<(EmailCallBackParam) -> Unit> =
        ArgumentCaptor.forClass(Function1::class.java) as ArgumentCaptor<(EmailCallBackParam) -> Unit>

    @Test
    fun wiring_publishMethodAndContactWay() {
        assertEquals(MsgPublishMethodEnum.EMAIL.listenerType, listener().notifyType())
    }

    @Test
    fun doDispatch_buildsRequestAndMapsAllSuccess() {
        val l = listener()
        // u1 and u2 share an address (team mailbox); u3 distinct
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(linkedMapOf("u1" to "team@x.com", "u2" to "team@x.com", "u3" to "c@x.com"))
        whenCalled(receiveService.insert(anyNN(MsgReceive::class.java))).thenReturn("r")

        val cbCaptor = emailCbCaptor()
        val reqCaptor = ArgumentCaptor.forClass(EmailRequest::class.java)

        l.notifyProcess(vo(event(setOf("u1", "u2", "u3"))))

        verify(emailHandler).send(reqCaptor.capture() ?: EmailRequest(), cbCaptor.capture() ?: { _ -> })
        val req = reqCaptor.value
        assertEquals("Subject", req.subject)
        assertEquals("Body", req.body)
        assertEquals("smtp.test", req.serverHost)
        assertEquals(465, req.serverPort)
        assertEquals("notify@test", req.senderAccount)
        // deduped recipient set: team@x.com once + c@x.com
        assertEquals(setOf("team@x.com", "c@x.com"), req.receivers)

        // invoke callback: all addresses succeed
        val cb = EmailCallBackParam().apply {
            successEmails = mutableSetOf("team@x.com", "c@x.com")
            failEmails = mutableSetOf()
        }
        cbCaptor.value.invoke(cb)

        // team@x.com maps back to u1+u2, c@x.com to u3 -> 3 successes, 0 fail -> SUCCESS
        verify(receiveService, times(3)).insert(anyNN(MsgReceive::class.java))
        verify(sendService).finishSend("send-1", 3, 0, MsgSendStatusEnum.SUCCESS.dictCode)
    }

    @Test
    fun doDispatch_failEmailsMappedToChannelReject() {
        val l = listener()
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(linkedMapOf("u1" to "a@x.com", "u2" to "b@x.com"))
        whenCalled(receiveService.insert(anyNN(MsgReceive::class.java))).thenReturn("r")

        val cbCaptor = emailCbCaptor()
        l.notifyProcess(vo(event(setOf("u1", "u2"))))
        verify(emailHandler).send(anyNN(EmailRequest::class.java), cbCaptor.capture() ?: { _ -> })

        val cb = EmailCallBackParam().apply {
            successEmails = mutableSetOf("a@x.com")
            failEmails = mutableSetOf("b@x.com")
        }
        cbCaptor.value.invoke(cb)

        verify(sendService).finishSend("send-1", 1, 1, MsgSendStatusEnum.SUCCESS_PARTIAL.dictCode)
    }

    @Test
    fun doDispatch_nullCallbackEmailListsTreatedAsEmpty() {
        val l = listener()
        whenCalled(contactService.getActiveContactValuesByUserIds(anyCollection(), eqNN("201")))
            .thenReturn(linkedMapOf("u1" to "a@x.com"))

        val cbCaptor = emailCbCaptor()
        l.notifyProcess(vo(event(setOf("u1"))))
        verify(emailHandler).send(anyNN(EmailRequest::class.java), cbCaptor.capture() ?: { _ -> })

        // both lists null -> orEmpty() -> 0 success, 0 fail; unreachable=0 -> successCount==0 -> FAILED_FINAL
        cbCaptor.value.invoke(EmailCallBackParam())
        verify(sendService).finishSend("send-1", 0, 0, MsgSendStatusEnum.FAILED_FINAL.dictCode)
    }
}
