package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiverGroupProxy
import org.springframework.cloud.openfeign.FallbackFactory
import org.springframework.stereotype.Component


/**
 * [MsgReceiverGroupFallback] 的 FallbackFactory。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class MsgReceiverGroupFallbackFactory : FallbackFactory<IMsgReceiverGroupProxy> {
    override fun create(cause: Throwable): IMsgReceiverGroupProxy = MsgReceiverGroupFallback(cause)
}
