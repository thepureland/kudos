package io.kudos.ms.msg.client.instance.proxy

import io.kudos.ms.msg.client.instance.fallback.MsgInstanceFallback
import io.kudos.ms.msg.common.instance.api.IMsgInstanceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 消息实例客户端代理接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "msg-instance", fallback = MsgInstanceFallback::class)
interface IMsgInstanceProxy : IMsgInstanceApi
