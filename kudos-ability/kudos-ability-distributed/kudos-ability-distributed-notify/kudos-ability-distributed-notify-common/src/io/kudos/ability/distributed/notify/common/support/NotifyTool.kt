package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable

/**
 * 通知工具类
 * 提供统一的通知消息发送接口，支持通过INotifyProducer实现类发送通知消息
 */
class NotifyTool {

    private val log = LogFactory.getLog(this)

    @Autowired(required = false)
    private val notifyProducer: INotifyProducer? = null

    fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
        if (notifyProducer != null) {
            return notifyProducer.notify(messageVo)
        } else {
            log.warn("未引入NotifyProduce实现..")
            return false
        }
    }

}
