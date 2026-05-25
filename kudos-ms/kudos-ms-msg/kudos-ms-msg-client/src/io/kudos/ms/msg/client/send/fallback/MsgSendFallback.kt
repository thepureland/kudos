package io.kudos.ms.msg.client.send.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.send.proxy.IMsgSendProxy
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest


/**
 * Feign fallback implementation for message send.
 *
 * publish is a write API; when the remote is unreachable, an error is logged and null is returned,
 * so the caller can queue a retry or apply business-side compensation.
 *
 * [MsgSendFallbackFactory] constructs a new instance on each fallback and passes in the triggering
 * [cause], so logs can distinguish 4xx (client error) / 5xx (server error) / unreachable (no response).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MsgSendFallback(
    private val cause: Throwable? = null,
) : AbstractFeignFallbackSupport("MsgSendFallback"), IMsgSendProxy {

    override fun publish(request: MsgPublishRequest): String? {
        errorWrite("publish", cause, request)
        return null
    }
}
