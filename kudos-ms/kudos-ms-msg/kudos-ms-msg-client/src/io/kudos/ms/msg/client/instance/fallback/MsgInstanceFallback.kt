package io.kudos.ms.msg.client.instance.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.msg.client.instance.proxy.IMsgInstanceProxy
import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry


/**
 * Fallback implementation for message instance.
 *
 * Each method comes in two shapes — see [MsgSendFallback] for why.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
open class MsgInstanceFallback : AbstractHttpFallbackSupport("MsgInstanceFallback"), IMsgInstanceProxy {

    fun getInstanceById(cause: Throwable?, id: String): MsgInstanceCacheEntry? {
        warnRead("getInstanceById", cause, id)
        return null
    }

    override fun getInstanceById(id: String): MsgInstanceCacheEntry? = getInstanceById(null, id)

}
