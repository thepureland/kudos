package io.kudos.ms.msg.core.send.service

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.enums.MsgSendStatusEnum
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.core.instance.model.po.MsgInstance
import io.kudos.ms.msg.core.instance.service.iservice.IMsgInstanceService
import io.kudos.ms.msg.core.send.model.po.MsgSend
import io.kudos.ms.msg.core.send.service.impl.MsgPublishService
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import io.kudos.ms.msg.core.support.MockitoKt.anyNN
import io.kudos.ms.msg.core.template.render.MsgTemplateRenderer
import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import org.springframework.beans.factory.ObjectProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for [MsgPublishService.publish] — exercises every branch without a Spring
 * container or DB, using Mockito mocks for all collaborators and a real [MsgTemplateRenderer].
 *
 * Covered branches:
 * - empty receiverIds -> null, nothing persisted
 * - idempotency hit (existing record) -> returns existing id, no insert
 * - idempotency key blank -> treated as no key (dedup disabled)
 * - template not found -> null
 * - producer unavailable -> status FAILED_TO_SEND_TO_MQ, sendId still returned
 * - producer.notify true -> status SENT_TO_MQ
 * - producer.notify false -> status FAILED_TO_SEND_TO_MQ
 * - producer.notify throws -> caught -> status FAILED_TO_SEND_TO_MQ
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgPublishServicePureTest {

    private val templateService = mock(IMsgTemplateService::class.java)
    private val instanceService = mock(IMsgInstanceService::class.java)
    private val sendService = mock(IMsgSendService::class.java)
    private val renderer = MsgTemplateRenderer()

    @Suppress("UNCHECKED_CAST")
    private fun build(producer: INotifyProducer?): MsgPublishService {
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<INotifyProducer>
        whenCalled(provider.ifAvailable).thenReturn(producer)
        val svc = MsgPublishService(provider)
        inject(svc, "msgTemplateService", templateService)
        inject(svc, "msgInstanceService", instanceService)
        inject(svc, "msgSendService", sendService)
        inject(svc, "renderer", renderer)
        return svc
    }

    private fun inject(target: Any, name: String, value: Any) {
        val f = MsgPublishService::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    private fun request(
        receiverIds: Set<String> = setOf("u1"),
        idempotencyKey: String? = null,
    ) = MsgPublishRequest(
        tenantId = "t1",
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "welcome",
        publishMethod = MsgPublishMethodEnum.EMAIL,
        receiverIds = receiverIds,
        params = mapOf("name" to "Alice"),
        idempotencyKey = idempotencyKey,
    )

    private fun template() = MsgTemplateCacheEntry(
        id = "tmpl-1",
        sendTypeDictCode = "01",
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "welcome",
        receiverGroupCode = null,
        localeDictCode = null,
        title = "Hi \${name}",
        content = "Welcome \${name}",
        defaultActive = true,
        defaultTitle = null,
        defaultContent = null,
        tenantId = "t1",
    )

    private fun stubHappyPath() {
        whenCalled(
            templateService.getTemplateByEvent(anyString(), anyString(), anyString(), any())
        ).thenReturn(template())
        whenCalled(instanceService.insert(anyNN(MsgInstance::class.java))).thenReturn("inst-1")
        whenCalled(sendService.insert(anyNN(MsgSend::class.java))).thenReturn("send-1")
    }

    @Test
    fun emptyReceiverIdsReturnsNullAndPersistsNothing() {
        val svc = build(mock(INotifyProducer::class.java))
        val result = svc.publish(request(receiverIds = emptySet()))
        assertNull(result)
        verify(instanceService, never()).insert(anyNN(MsgInstance::class.java))
        verify(sendService, never()).insert(anyNN(MsgSend::class.java))
    }

    @Test
    fun idempotencyHitReturnsExistingIdWithoutInserting() {
        val svc = build(mock(INotifyProducer::class.java))
        val existing = MsgSend().apply { id = "existing-send" }
        whenCalled(sendService.findByIdempotencyKey("t1", "key-1")).thenReturn(existing)

        val result = svc.publish(request(idempotencyKey = "key-1"))
        assertEquals("existing-send", result)
        verify(instanceService, never()).insert(anyNN(MsgInstance::class.java))
        verify(sendService, never()).insert(anyNN(MsgSend::class.java))
    }

    @Test
    fun blankIdempotencyKeyDisablesDedupAndProceeds() {
        val svc = build(producer = trueProducer())
        stubHappyPath()
        val result = svc.publish(request(idempotencyKey = "   "))
        assertEquals("send-1", result)
        // blank key never triggers a lookup
        verify(sendService, never()).findByIdempotencyKey(anyString(), anyString())
    }

    @Test
    fun templateNotFoundReturnsNull() {
        val svc = build(mock(INotifyProducer::class.java))
        whenCalled(
            templateService.getTemplateByEvent(anyString(), anyString(), anyString(), any())
        ).thenReturn(null)
        val result = svc.publish(request())
        assertNull(result)
        verify(instanceService, never()).insert(anyNN(MsgInstance::class.java))
    }

    @Test
    fun producerUnavailableMarksFailedToSendToMq() {
        val svc = build(producer = null)
        stubHappyPath()
        val result = svc.publish(request())
        assertEquals("send-1", result)
        verify(sendService).updateSendStatus("send-1", MsgSendStatusEnum.FAILED_TO_SEND_TO_MQ.dictCode)
    }

    @Test
    fun producerNotifyTrueMarksSentToMq() {
        val svc = build(producer = trueProducer())
        stubHappyPath()
        val result = svc.publish(request())
        assertEquals("send-1", result)
        verify(sendService).updateSendStatus("send-1", MsgSendStatusEnum.SENT_TO_MQ.dictCode)
    }

    @Test
    fun producerNotifyFalseMarksFailedToSendToMq() {
        val producer = mock(INotifyProducer::class.java)
        whenCalled(producer.notify(anyNN(NotifyMessageVo::class.java))).thenReturn(false)
        val svc = build(producer)
        stubHappyPath()
        val result = svc.publish(request())
        assertEquals("send-1", result)
        verify(sendService).updateSendStatus("send-1", MsgSendStatusEnum.FAILED_TO_SEND_TO_MQ.dictCode)
    }

    @Test
    fun producerNotifyThrowsIsCaughtAndMarksFailedToSendToMq() {
        val producer = mock(INotifyProducer::class.java)
        whenCalled(producer.notify(anyNN(NotifyMessageVo::class.java))).thenThrow(RuntimeException("mq down"))
        val svc = build(producer)
        stubHappyPath()
        val result = svc.publish(request())
        assertEquals("send-1", result)
        verify(sendService).updateSendStatus("send-1", MsgSendStatusEnum.FAILED_TO_SEND_TO_MQ.dictCode)
    }

    private fun trueProducer(): INotifyProducer {
        val producer = mock(INotifyProducer::class.java)
        whenCalled(producer.notify(anyNN(NotifyMessageVo::class.java))).thenReturn(true)
        return producer
    }
}
