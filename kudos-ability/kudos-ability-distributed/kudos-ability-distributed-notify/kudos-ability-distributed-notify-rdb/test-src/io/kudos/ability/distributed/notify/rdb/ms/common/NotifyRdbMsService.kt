package io.kudos.ability.distributed.notify.rdb.ms.common

import io.kudos.ability.distributed.notify.rdb.common.NotifyTypeEnum
import org.soul.ability.distributed.notify.common.api.INotifyProducer
import org.soul.ability.distributed.notify.common.model.NotifyMessageVo
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable

@Service
class NotifyRdbMsService {
    private val log: Log? = LogFactory.getLog(NotifyRdbMsService::class.java)

    @Autowired
    private val notifyProducer: INotifyProducer? = null

    fun process(key: String?): Boolean {
        val messageVo: NotifyMessageVo<*> = NotifyMessageVo<Any?>()
        messageVo.setNotifyType(NotifyTypeEnum.DS.getCode())
        messageVo.setMessageBody(key)
        return notifyProducer!!.notify<Serializable?>(messageVo)
    }
}
