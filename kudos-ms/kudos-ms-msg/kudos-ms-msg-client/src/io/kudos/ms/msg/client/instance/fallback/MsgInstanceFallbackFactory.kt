package io.kudos.ms.msg.client.instance.fallback

import io.kudos.ms.msg.client.instance.proxy.IMsgInstanceProxy
import org.springframework.cloud.openfeign.FallbackFactory
import org.springframework.stereotype.Component


/**
 * FallbackFactory for [MsgInstanceFallback].
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class MsgInstanceFallbackFactory : FallbackFactory<IMsgInstanceProxy> {
    override fun create(cause: Throwable): IMsgInstanceProxy = MsgInstanceFallback(cause)
}
