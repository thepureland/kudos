package io.kudos.ms.msg.client.send.fallback

import io.kudos.ms.msg.client.send.proxy.IMsgSendProxy
import org.springframework.cloud.openfeign.FallbackFactory
import org.springframework.stereotype.Component


/**
 * FallbackFactory for [MsgSendFallback].
 *
 * Spring Cloud OpenFeign calls [create] when a remote invocation fails, passing the triggering
 * exception to this factory, which constructs a [MsgSendFallback] instance capable of logging
 * 4xx / 5xx / unreachable categories.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class MsgSendFallbackFactory : FallbackFactory<IMsgSendProxy> {
    override fun create(cause: Throwable): IMsgSendProxy = MsgSendFallback(cause)
}
