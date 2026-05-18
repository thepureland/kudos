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
 * [IMsgPublishService] 默认实现。
 *
 * INotifyProducer 注入用 ObjectProvider：notify-mq 在 deployment 模块里才被引入；
 * msg-core 单独跑（如在 kudos-ms-msg-core 测试上下文）时 producer 不可达 —— 此时
 * MQ 投递失败 → MsgSend.status = FAILED_TO_SEND_TO_MQ；service 仍能返回 sendId 让
 * 调用方知道记录已落库。
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
            log.warn("publish 入参 receiverIds 为空，忽略")
            return null
        }

        val template = msgTemplateService.getTemplateByEvent(
            tenantId = request.tenantId,
            eventTypeDictCode = request.eventTypeDictCode,
            msgTypeDictCode = request.msgTypeDictCode,
            localeDictCode = request.localeDictCode,
        )
        if (template == null) {
            log.warn(
                "找不到模板，发送取消：tenantId={0}, eventType={1}, msgType={2}, locale={3}",
                request.tenantId, request.eventTypeDictCode, request.msgTypeDictCode, request.localeDictCode,
            )
            return null
        }

        val rendered = renderer.render(template, request.params)

        // 落 MsgInstance 快照
        val now = LocalDateTime.now()
        val instance = MsgInstance().apply {
            localeDictCode = request.localeDictCode
            title = rendered.title
            content = rendered.content
            templateId = template.id
            sendTypeDictCode = template.sendTypeDictCode
            eventTypeDictCode = request.eventTypeDictCode
            msgTypeDictCode = request.msgTypeDictCode
            // 有效期：默认本次 publish 开始 ~ 30 天后；admin 可后续调整
            validTimeStart = now
            validTimeEnd = now.plusDays(30)
            tenantId = request.tenantId
        }
        val instanceId = msgInstanceService.insert(instance)

        // 落 MsgSend，status=PENDING
        val send = MsgSend().apply {
            receiverGroupTypeDictCode = "user" // 仅支持用户级派发；扩展时改为 request.receiverGroupType
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
            tenantId = request.tenantId
        }
        val sendId = msgSendService.insert(send)

        // 投 MQ
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
            log.warn("notify producer 不可用（未引入 notify-mq？），消息 {0} 未投递", sendId)
            MsgSendStatusEnum.FAILED_TO_SEND_TO_MQ
        } else {
            val ok = runCatching { producer.notify(notifyMsg) }.getOrElse {
                log.error(it, "投递 notify 异常：sendId={0}", sendId)
                false
            }
            if (ok) MsgSendStatusEnum.SENT_TO_MQ else MsgSendStatusEnum.FAILED_TO_SEND_TO_MQ
        }
        msgSendService.updateSendStatus(sendId, newStatus.dictCode)

        return sendId
    }
}
