package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiveProxy
import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry


/**
 * Fallback implementation for message receive.
 *
 * Write APIs (`markRead` / `markAllReadByUserId`) go through errorWrite and return false/0 on
 * failure; the caller should retry later or surface the failure to the user.
 *
 * Each method comes in two shapes — see [MsgSendFallback] for why.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
open class MsgReceiveFallback : AbstractHttpFallbackSupport("MsgReceiveFallback"), IMsgReceiveProxy {

    fun getReceivesByUserId(cause: Throwable?, receiverId: String): List<MsgReceiveCacheEntry> {
        warnRead("getReceivesByUserId", cause, receiverId)
        return emptyList()
    }

    override fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry> =
        getReceivesByUserId(null, receiverId)

    fun getUnreadCountByUserId(cause: Throwable?, receiverId: String): Int {
        warnRead("getUnreadCountByUserId", cause, receiverId)
        return 0
    }

    override fun getUnreadCountByUserId(receiverId: String): Int = getUnreadCountByUserId(null, receiverId)

    fun markRead(cause: Throwable?, id: String): Boolean {
        errorWrite("markRead", cause, id)
        return false
    }

    override fun markRead(id: String): Boolean = markRead(null, id)

    fun markAllReadByUserId(cause: Throwable?, receiverId: String): Int {
        errorWrite("markAllReadByUserId", cause, receiverId)
        return 0
    }

    override fun markAllReadByUserId(receiverId: String): Int = markAllReadByUserId(null, receiverId)

}
