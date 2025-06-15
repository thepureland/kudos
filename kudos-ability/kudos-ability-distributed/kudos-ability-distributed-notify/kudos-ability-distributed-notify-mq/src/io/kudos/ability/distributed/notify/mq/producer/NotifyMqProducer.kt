package io.kudos.ability.distributed.notify.mq.producer

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.stream.common.annotations.MqProducer
import java.io.Serializable


/**
 * 创建人： Younger
 * 日期： 2022/11/14 15:11
 * 描述：
 */
open class NotifyMqProducer : INotifyProducer {

    @MqProducer(topic = "mqNotify", bindingName = "mqNotify-out-0")
    override fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean {
        return true
    }

}
