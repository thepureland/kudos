package io.kudos.ability.distributed.notify.common.api

import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import java.io.Serializable


/**
 * SPI for consumers of cross-node notification messages.
 *
 * Callers implement this interface and register it as a Spring bean—[NotifyListenerBeanPostProcessor]
 * automatically registers it into [NotifyListenerItem] keyed by [notifyType]. When a notification arrives
 * (delivered by modules such as notify-mq) it is dispatched by type.
 *
 * Typical implementation: `io.kudos.ability.cache.common.notify.CacheNotifyListener` (receives cache invalidation messages).
 *
 * @author Younger
 * @author K
 * @since 1.0.0
 */
interface INotifyListener {

    /** Identifier of the notification type this listener handles. When multiple listeners share the same type, they are distinguished by namespace. */
    fun notifyType(): String

    /** Handles the received notification message. Callers must implement idempotency themselves—MQ implementations may deliver multiple times. */
    fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>)

}
