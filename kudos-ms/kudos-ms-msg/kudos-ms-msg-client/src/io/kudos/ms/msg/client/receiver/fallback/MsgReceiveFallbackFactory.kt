package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiveProxy
import org.springframework.cloud.openfeign.FallbackFactory
import org.springframework.stereotype.Component


/**
 * [MsgReceiveFallback] 的 FallbackFactory。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class MsgReceiveFallbackFactory : FallbackFactory<IMsgReceiveProxy> {
    override fun create(cause: Throwable): IMsgReceiveProxy = MsgReceiveFallback(cause)
}
