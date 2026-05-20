package io.kudos.ms.msg.client.receiver.proxy

import io.kudos.ms.msg.client.receiver.fallback.MsgReceiverGroupFallback
import io.kudos.ms.msg.common.receiver.api.IMsgReceiverGroupApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 消息接收者群组客户端代理接口。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "msg-receiver-group", fallback = MsgReceiverGroupFallback::class)
interface IMsgReceiverGroupProxy : IMsgReceiverGroupApi
