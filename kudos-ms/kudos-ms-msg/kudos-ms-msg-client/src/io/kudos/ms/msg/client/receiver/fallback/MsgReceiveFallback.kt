package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiveProxy
import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry


/**
 * Feign fallback implementation for message receive.
 *
 * Write APIs ([markRead] / [markAllReadByUserId]) go through errorWrite and return false/0 on
 * failure; the caller should retry later or surface the failure to the user.
 *
 * [MsgReceiveFallbackFactory] constructs a new instance on each fallback and passes in [cause],
 * so logs can distinguish 4xx / 5xx / unreachable.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MsgReceiveFallback(
    private val cause: Throwable? = null,
) : AbstractFeignFallbackSupport("MsgReceiveFallback"), IMsgReceiveProxy {

    override fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry> {
        warnRead("getReceivesByUserId", cause, receiverId)
        return emptyList()
    }

    override fun getUnreadCountByUserId(receiverId: String): Int {
        warnRead("getUnreadCountByUserId", cause, receiverId)
        return 0
    }

    override fun markRead(id: String): Boolean {
        errorWrite("markRead", cause, id)
        return false
    }

    override fun markAllReadByUserId(receiverId: String): Int {
        errorWrite("markAllReadByUserId", cause, receiverId)
        return 0
    }
}
