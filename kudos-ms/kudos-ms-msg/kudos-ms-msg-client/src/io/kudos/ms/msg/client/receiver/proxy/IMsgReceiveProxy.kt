package io.kudos.ms.msg.client.receiver.proxy

import io.kudos.ms.msg.client.receiver.fallback.MsgReceiveFallbackFactory
import io.kudos.ms.msg.common.receiver.api.IMsgReceiveApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Message receive client proxy interface (inbox RPC view).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "msg-receive", fallbackFactory = MsgReceiveFallbackFactory::class)
interface IMsgReceiveProxy : IMsgReceiveApi
