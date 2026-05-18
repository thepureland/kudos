package io.kudos.ms.msg.client.receiver.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.receiver.proxy.IMsgReceiveProxy
import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import org.springframework.stereotype.Component


/**
 * 消息接收 Feign 容错降级实现。
 *
 * 写接口（[markRead] / [markAllReadByUserId]）走 errorWrite，返回 false/0 表示失败，
 * 调用方应据此选择稍后重试或提示用户。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class MsgReceiveFallback :
    AbstractFeignFallbackSupport("MsgReceiveFallback"), IMsgReceiveProxy {

    override fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry> {
        warnRead("getReceivesByUserId", receiverId)
        return emptyList()
    }

    override fun getUnreadCountByUserId(receiverId: String): Int {
        warnRead("getUnreadCountByUserId", receiverId)
        return 0
    }

    override fun markRead(id: String): Boolean {
        errorWrite("markRead", id)
        return false
    }

    override fun markAllReadByUserId(receiverId: String): Int {
        errorWrite("markAllReadByUserId", receiverId)
        return 0
    }
}
