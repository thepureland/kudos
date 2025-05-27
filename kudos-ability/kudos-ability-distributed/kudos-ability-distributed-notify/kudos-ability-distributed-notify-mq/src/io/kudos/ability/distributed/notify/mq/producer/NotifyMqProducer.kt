package io.kudos.ability.distributed.notify.mq.producer

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.stream.common.annotations.MqProducer


/**
 * 创建人： Younger
 * 日期： 2022/11/14 15:11
 * 描述：
 */
class NotifyMqProducer : INotifyProducer {
    @MqProducer(topic = "mqNotify", bindingName = "mqNotify-out-0")
    override fun notify(messageVo: NotifyMessageVo<*>?): Boolean {
        return true
    }
}
