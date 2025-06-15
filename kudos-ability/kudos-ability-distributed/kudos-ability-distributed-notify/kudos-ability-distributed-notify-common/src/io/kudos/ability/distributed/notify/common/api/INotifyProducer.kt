package io.kudos.ability.distributed.notify.common.api

import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable


/**
 * 创建人： Younger
 * 日期： 2022/11/14 16:21
 * 描述：
 */
interface INotifyProducer {

    /**
     * 集群节点消息发送
     *
     * @param messageVo
     */
    fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean

    companion object {
        const val BEAN_NAME: String = "notifyMqProducer"
    }

}
