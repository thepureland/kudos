package io.kudos.ability.distributed.notify.support

import io.kudos.ability.distributed.notify.api.INotifyProducer
import io.kudos.ability.distributed.notify.model.NotifyMessageVo
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class NotifyTool {
    @Autowired(required = false)
    private val notifyProducer: INotifyProducer? = null

    fun notify(messageVo: NotifyMessageVo<*>?): Boolean {
        if (notifyProducer != null) {
            return notifyProducer.notify<Serializable?>(messageVo)
        } else {
            log.warn("未引入NotifyProduce实现..")
            return false
        }
    }

    companion object {
        private val log: Log = LogFactory.getLog(NotifyTool::class.java)
    }
}
