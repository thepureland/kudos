package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import java.io.Serializable

/**
 * 通知工具类
 * 提供统一的通知消息发送接口，支持通过INotifyProducer实现类发送通知消息
 */
class NotifyTool(
    private val notifyProducer: INotifyProducer?,
    private val properties: NotifyCommonProperties
) {

    private val log = LogFactory.getLog(this::class)

    fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
        if (notifyProducer != null) {
            return notifyProducer.notify(messageVo)
        } else {
            val msg = "未引入NotifyProduce实现.."
            if (properties.failOnMissingProducer) {
                throw IllegalStateException(msg)
            }
            log.warn(msg)
            return false
        }
    }

}
