package io.kudos.ability.distributed.notify.rdb.ms.common

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.rdb.common.NotifyTypeEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class NotifyRdbMsService {

    @Autowired
    private lateinit var notifyProducer: INotifyProducer

    fun process(key: String?): Boolean {
        val messageVo = NotifyMessageVo<String>()
        messageVo.notifyType = NotifyTypeEnum.DS.code
        messageVo.messageBody = key
        return notifyProducer.notify(messageVo)
    }
}
