package io.kudos.ms.msg.client.instance.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.instance.proxy.IMsgInstanceProxy
import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry


/**
 * Feign fallback implementation for message instance.
 *
 * [MsgInstanceFallbackFactory] constructs a new instance on each fallback and passes in [cause],
 * so logs can distinguish 4xx / 5xx / unreachable.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MsgInstanceFallback(
    private val cause: Throwable? = null,
) : AbstractFeignFallbackSupport("MsgInstanceFallback"), IMsgInstanceProxy {

    override fun getInstanceById(id: String): MsgInstanceCacheEntry? {
        warnRead("getInstanceById", cause, id)
        return null
    }
}
