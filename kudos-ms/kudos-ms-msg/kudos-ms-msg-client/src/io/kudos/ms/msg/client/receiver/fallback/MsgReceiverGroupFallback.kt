package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiverGroupProxy
import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry


/**
 * Fallback implementation for message receiver groups.
 *
 * Each method comes in two shapes — see [MsgSendFallback] for why.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
open class MsgReceiverGroupFallback :
    AbstractHttpFallbackSupport("MsgReceiverGroupFallback"),
    IMsgReceiverGroupProxy {

    fun getReceiverGroupById(cause: Throwable?, id: String): MsgReceiverGroupCacheEntry? {
        warnRead("getReceiverGroupById", cause, id)
        return null
    }

    override fun getReceiverGroupById(id: String): MsgReceiverGroupCacheEntry? =
        getReceiverGroupById(null, id)

    fun listActiveReceiverGroups(
        cause: Throwable?,
        receiverGroupTypeDictCode: String?
    ): List<MsgReceiverGroupCacheEntry> {
        warnRead("listActiveReceiverGroups", cause, receiverGroupTypeDictCode)
        return emptyList()
    }

    override fun listActiveReceiverGroups(
        receiverGroupTypeDictCode: String?
    ): List<MsgReceiverGroupCacheEntry> = listActiveReceiverGroups(null, receiverGroupTypeDictCode)

}
