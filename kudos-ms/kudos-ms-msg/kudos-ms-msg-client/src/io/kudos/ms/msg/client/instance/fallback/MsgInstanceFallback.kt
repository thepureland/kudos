package io.kudos.ms.msg.client.instance.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.instance.proxy.IMsgInstanceProxy
import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import org.springframework.stereotype.Component


/**
 * 消息实例 Feign 容错降级实现。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class MsgInstanceFallback :
    AbstractFeignFallbackSupport("MsgInstanceFallback"), IMsgInstanceProxy {

    override fun getInstanceById(id: String): MsgInstanceCacheEntry? {
        warnRead("getInstanceById", id)
        return null
    }
}
