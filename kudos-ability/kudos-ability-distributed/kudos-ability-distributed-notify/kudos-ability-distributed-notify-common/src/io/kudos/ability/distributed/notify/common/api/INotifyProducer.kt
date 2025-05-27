package io.kudos.ability.distributed.notify.api

import io.kudos.ability.distributed.notify.model.NotifyMessageVo
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
    fun <T : Serializable?> notify(messageVo: NotifyMessageVo<T?>?): Boolean

    companion object {
        const val BEAN_NAME: String = "notifyMqProduce"
    }
}
