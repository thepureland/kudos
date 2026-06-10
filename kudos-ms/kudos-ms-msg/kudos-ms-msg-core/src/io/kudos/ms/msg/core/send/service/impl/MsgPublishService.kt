package io.kudos.ms.msg.core.send.service.impl

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import io.kudos.ms.msg.common.send.enums.MsgSendStatusEnum
import io.kudos.ms.msg.common.send.vo.MsgDispatchEvent
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import io.kudos.ms.msg.core.instance.model.po.MsgInstance
import io.kudos.ms.msg.core.send.model.po.MsgSend
import io.kudos.ms.msg.core.send.service.iservice.IMsgPublishService
import io.kudos.ms.msg.core.instance.service.iservice.IMsgInstanceService
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import io.kudos.ms.msg.core.template.render.MsgTemplateRenderer
import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
import jakarta.annotation.Resource
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * Default implementation of [IMsgPublishService].
 *
 * INotifyProducer is injected via ObjectProvider: notify-mq is only imported in the deployment module;
 * when msg-core is run standalone (e.g. in the kudos-ms-msg-core test context) the producer is unreachable —
 * in that case MQ delivery fails → MsgSend.status = FAILED_TO_SEND_TO_MQ; the service still returns the sendId
 * so the caller knows the record has been persisted.
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class MsgPublishService(
    private val notifyProducerProvider: ObjectProvider<INotifyProducer>,
) : IMsgPublishService {

    @Resource
    private lateinit var msgTemplateService: IMsgTemplateService

    @Resource
    private lateinit var msgInstanceService: IMsgInstanceService

    @Resource
    private lateinit var msgSendService: IMsgSendService

    @Resource
    private lateinit var renderer: MsgTemplateRenderer

    private val log = LogFactory.getLog(this::class)

    override fun publish(request: MsgPublishRequest): String? {
        if (request.receiverIds.isEmpty()) {
            log.warn("publish input receiverIds is empty, ignoring")
            return null
        }

        // Idempotency short-circuit: if the caller supplied an idempotencyKey and a prior publish with the
        // same (tenantId, idempotencyKey) already persisted a MsgSend, return that id instead of creating
        // duplicate instance/send records. Concurrent races are caught by the unique index
        // uq_msg_send__tenant_idempotency — the losing transaction fails and, on retry, hits this branch.
        val idempotencyKey = request.idempotencyKey?.takeIf { it.isNotBlank() }
        if (idempotencyKey != null) {
            val existing = msgSendService.findByIdempotencyKey(request.tenantId, idempotencyKey)
            if (existing != null) {
                log.info("Idempotent publish hit: tenantId={0}, key={1}, existing sendId={2}", request.tenantId, idempotencyKey, existing.id)
                return existing.id
            }
        }

        val template = msgTemplateService.getTemplateByEvent(
            tenantId = request.tenantId,
            eventTypeDictCode = request.eventTypeDictCode,
            msgTypeDictCode = request.msgTypeDictCode,
            localeDictCode = request.localeDictCode,
        )
        if (template == null) {
            log.warn(
                "Template not found, send canceled: tenantId={0}, eventType={1}, msgType={2}, locale={3}",
                request.tenantId, request.eventTypeDictCode, request.msgTypeDictCode, request.localeDictCode,
            )
            return null
        }

        val rendered = renderer.render(template, request.params)

        // Persist MsgInstance snapshot
        val now = LocalDateTime.now()
        val instance = MsgInstance().apply {
            localeDictCode = request.localeDictCode
            title = rendered.title
            content = rendered.content
            templateId = template.id
            sendTypeDictCode = template.sendTypeDictCode
            eventTypeDictCode = request.eventTypeDictCode
            msgTypeDictCode = request.msgTypeDictCode
            // Validity: defaults to from the start of this publish ~ 30 days later; admin can adjust later
            validTimeStart = now
            validTimeEnd = now.plusDays(30)
            tenantId = request.tenantId
        }
        val instanceId = msgInstanceService.insert(instance)

        // Persist MsgSend, status=PENDING
        val send = MsgSend().apply {
            receiverGroupTypeDictCode = RECEIVER_GROUP_TYPE_USER // Only supports user-level dispatch; switch to request.receiverGroupType when extending
            receiverGroupId = null
            this.instanceId = instanceId
            msgTypeDictCode = request.msgTypeDictCode
            localeDictCode = request.localeDictCode
            sendStatusDictCode = MsgSendStatusEnum.PENDING.dictCode
            createTime = now
            updateTime = null
            successCount = 0
            failCount = 0
            jobId = null
            this.idempotencyKey = idempotencyKey
            tenantId = request.tenantId
        }
        val sendId = msgSendService.insert(send)

        // Send to MQ
        val event = MsgDispatchEvent(
            sendId = sendId,
            instanceId = instanceId,
            publishMethodDictCode = request.publishMethod.dictCode,
            receiverIds = request.receiverIds,
            renderedTitle = rendered.title,
            renderedContent = rendered.content,
            localeDictCode = request.localeDictCode,
            tenantId = request.tenantId,
        )
        val notifyMsg = NotifyMessageVo(request.publishMethod.listenerType, event)
        val producer = notifyProducerProvider.ifAvailable
        val newStatus = if (producer == null) {
            log.warn("notify producer is unavailable (notify-mq not imported?), message {0} not dispatched", sendId)
            MsgSendStatusEnum.FAILED_TO_SEND_TO_MQ
        } else {
            val ok = runCatching { producer.notify(notifyMsg) }.getOrElse {
                log.error(it, "Notify dispatch exception: sendId={0}", sendId)
                false
            }
            if (ok) MsgSendStatusEnum.SENT_TO_MQ else MsgSendStatusEnum.FAILED_TO_SEND_TO_MQ
        }
        msgSendService.updateSendStatus(sendId, newStatus.dictCode)

        return sendId
    }

    companion object {
        /** `receiver_group_type` dict itemCode for direct user-level dispatch (see V1.0.0.2__insert_sys_dict_item.sql). */
        private const val RECEIVER_GROUP_TYPE_USER = "user"
    }
}
