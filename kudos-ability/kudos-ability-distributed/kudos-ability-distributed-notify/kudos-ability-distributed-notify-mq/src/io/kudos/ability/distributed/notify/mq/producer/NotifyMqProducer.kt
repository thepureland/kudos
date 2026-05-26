package io.kudos.ability.distributed.notify.mq.producer

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.mq.support.NotifyMqBindings
import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import io.kudos.base.logger.LogFactory
import java.io.Serializable


/**
 * MQ-based [INotifyProducer] implementation.
 *
 * **Key design**: same "AOP placeholder" pattern as `kudos-ability-log-audit-mq.MqAuditService` —
 * the `return true` in the method body is a placeholder; the actual send is performed by the
 * `@MqProducer` aspect (in the stream-common module), which intercepts the messageVo parameter
 * and routes it to spring-cloud-stream's `mqNotify-out-0` binding.
 *
 * **If the application does not include the MQ producer aspect from `kudos-ability-distributed-stream-*`,
 * this method is effectively a no-op** — notifications are silently dropped.
 *
 * @author Younger
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class NotifyMqProducer : INotifyProducer {

    private val log = LogFactory.getLog(this::class)

    @MqProducer(topic = NotifyMqBindings.TOPIC, bindingName = NotifyMqBindings.PRODUCER_BINDING)
    override fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
        if (messageVo.notifyType.isBlank()) {
            log.warn("notifyType is blank, cancelling notification send")
            return false
        }
        return true
    }

}
