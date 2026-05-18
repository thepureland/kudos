package io.kudos.ms.msg.client.send.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.msg.client.send.proxy.IMsgSendProxy
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import org.springframework.stereotype.Component


/**
 * 消息发送 Feign 容错降级实现。
 *
 * publish 是写接口；远端不可达时记 error 并返回 null，调用方可据此排队重试或落业务侧补偿。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class MsgSendFallback :
    AbstractFeignFallbackSupport("MsgSendFallback"), IMsgSendProxy {

    override fun publish(request: MsgPublishRequest): String? {
        errorWrite("publish", request)
        return null
    }
}
