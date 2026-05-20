package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiverGroupProxy
import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import org.springframework.stereotype.Component


/**
 * 消息接收者群组 Feign 容错降级实现。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class MsgReceiverGroupFallback :
    AbstractFeignFallbackSupport("MsgReceiverGroupFallback"), IMsgReceiverGroupProxy {

    override fun getReceiverGroupById(id: String): MsgReceiverGroupCacheEntry? {
        warnRead("getReceiverGroupById", id)
        return null
    }

    override fun listActiveReceiverGroups(receiverGroupTypeDictCode: String?): List<MsgReceiverGroupCacheEntry> {
        warnRead("listActiveReceiverGroups", receiverGroupTypeDictCode)
        return emptyList()
    }

}
