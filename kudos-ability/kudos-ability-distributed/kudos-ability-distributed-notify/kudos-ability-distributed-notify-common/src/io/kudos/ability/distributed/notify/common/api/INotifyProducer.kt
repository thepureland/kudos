package io.kudos.ability.distributed.notify.common.api

import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable


/**
 * 跨节点通知消息发送方 SPI。
 *
 * 业务侧调 `NotifyTool.notify(messageVo)` 间接触发；具体投递实现在 `notify-mq` 等模块
 * （bean 名 [BEAN_NAME]）。常用于 cache 失效广播、配置变更通知、多节点状态同步。
 *
 * @author Younger
 * @author K
 * @since 1.0.0
 */
interface INotifyProducer {

    /**
     * 集群节点消息发送。返回 true 表示发送成功；具体语义看实现（at-least-once / fire-and-forget）。
     *
     * @param messageVo 通知消息载体（含 notifyType + messageBody）
     */
    fun notify(messageVo: NotifyMessageVo<out Serializable>): Boolean

    companion object {
        /** Spring bean 名约定——多实现切换时按此 name 取。 */
        const val BEAN_NAME: String = "notifyMqProducer"
    }

}
