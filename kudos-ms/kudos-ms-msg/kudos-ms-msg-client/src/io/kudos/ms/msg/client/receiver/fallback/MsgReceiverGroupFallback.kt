package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiverGroupProxy
import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry


/**
 * Feign fallback implementation for message receiver groups.
 *
 * Instantiated by [MsgReceiverGroupFallbackFactory] on each fallback with [cause] supplied,
 * so logs can distinguish 4xx / 5xx / unreachable.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MsgReceiverGroupFallback(
    private val cause: Throwable? = null,
) : AbstractFeignFallbackSupport("MsgReceiverGroupFallback"), IMsgReceiverGroupProxy {

    override fun getReceiverGroupById(id: String): MsgReceiverGroupCacheEntry? {
        warnRead("getReceiverGroupById", cause, id)
        return null
    }

    override fun listActiveReceiverGroups(receiverGroupTypeDictCode: String?): List<MsgReceiverGroupCacheEntry> {
        warnRead("listActiveReceiverGroups", cause, receiverGroupTypeDictCode)
        return emptyList()
    }

}
