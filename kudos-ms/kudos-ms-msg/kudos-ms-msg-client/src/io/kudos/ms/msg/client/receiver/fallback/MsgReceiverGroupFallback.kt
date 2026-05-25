package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiverGroupProxy
import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry


/**
 * 消息接收者群组 Feign 容错降级实现。
 *
 * 由 [MsgReceiverGroupFallbackFactory] 在每次降级时构造一个实例并传入 [cause]，
 * 让日志能区分 4xx / 5xx / unreachable。
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
