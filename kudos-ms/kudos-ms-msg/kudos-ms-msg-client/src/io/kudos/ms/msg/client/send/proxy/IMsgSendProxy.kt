package io.kudos.ms.msg.client.send.proxy

import io.kudos.ms.msg.client.send.fallback.MsgSendFallbackFactory
import io.kudos.ms.msg.common.send.api.IMsgSendApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Message send client proxy interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "msg-send", fallbackFactory = MsgSendFallbackFactory::class)
interface IMsgSendProxy : IMsgSendApi
