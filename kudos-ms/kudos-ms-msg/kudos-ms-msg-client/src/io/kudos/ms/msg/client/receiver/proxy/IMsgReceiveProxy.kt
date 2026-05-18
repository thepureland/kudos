package io.kudos.ms.msg.client.receiver.proxy

import io.kudos.ms.msg.client.receiver.fallback.MsgReceiveFallback
import io.kudos.ms.msg.common.receiver.api.IMsgReceiveApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 消息接收客户端代理接口（收件箱 RPC 视图）
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "msg-receive", fallback = MsgReceiveFallback::class)
interface IMsgReceiveProxy : IMsgReceiveApi
