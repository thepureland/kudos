package io.kudos.ability.distributed.notify.mq.producer

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.mq.support.NotifyMqBindings
import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.base.logger.LogFactory
import java.io.Serializable


/**
 * MQ 投递的 [INotifyProducer] 实现。
 *
 * **关键设计**：与 `kudos-ability-log-audit-mq.MqAuditService` 同款"AOP 占位"模式 ——
 * 方法体里 `return true` 是占位；真正的发送由 `@MqProducer` 切面（在 stream-common 模块）
 * 拦截 messageVo 参数完成，路由到 spring-cloud-stream 的 `mqNotify-out-0` binding。
 *
 * **如果应用没引入 `kudos-ability-distributed-stream-*` 的 MQ producer 切面，本方法实际是
 * no-op**——通知静默丢失。
 *
 * @author Younger
 * @author K
 * @since 1.0.0
 */
open class NotifyMqProducer : INotifyProducer {

    private val log = LogFactory.getLog(this::class)

    @MqProducer(topic = NotifyMqBindings.TOPIC, bindingName = NotifyMqBindings.PRODUCER_BINDING)
    override fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
        if (messageVo.notifyType.isBlank()) {
            log.warn("notifyType 为空，取消发送通知")
            return false
        }
        return true
    }

}
