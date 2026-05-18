package io.kudos.ms.msg.client.send.proxy

import io.kudos.ms.msg.client.send.fallback.MsgSendFallback
import io.kudos.ms.msg.common.send.api.IMsgSendApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 消息发送 客户端代理接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "msg-send", fallback = MsgSendFallback::class)
interface IMsgSendProxy : IMsgSendApi
