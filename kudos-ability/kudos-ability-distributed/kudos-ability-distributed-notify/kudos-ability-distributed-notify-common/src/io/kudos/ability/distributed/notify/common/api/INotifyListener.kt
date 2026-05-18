package io.kudos.ability.distributed.notify.common.api

import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable


/**
 * 跨节点通知消息的消费方 SPI。
 *
 * 业务侧实现本接口并注册为 Spring bean——[NotifyListenerBeanPostProcessor] 会按 [notifyType]
 * 自动登记到 [NotifyListenerItem]。通知到达时（具体由 notify-mq 等模块负责）按 type 派发。
 *
 * 典型实现：`io.kudos.ability.cache.common.notify.CacheNotifyListener`（接收 cache 失效消息）。
 *
 * @author Younger
 * @author K
 * @since 1.0.0
 */
interface INotifyListener {

    /** 本 listener 关心的通知类型标识。多个 listener 关心同一 type 时按 namespace 区分。 */
    fun notifyType(): String

    /** 处理收到的通知消息。注意业务侧应自行做幂等——MQ 实现可能投递多次。 */
    fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>)

}
