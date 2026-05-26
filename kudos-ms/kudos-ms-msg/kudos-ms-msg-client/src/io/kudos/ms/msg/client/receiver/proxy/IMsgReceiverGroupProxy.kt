package io.kudos.ms.msg.client.receiver.proxy

import io.kudos.ms.msg.client.receiver.fallback.MsgReceiverGroupFallbackFactory
import io.kudos.ms.msg.common.receiver.api.IMsgReceiverGroupApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Message receiver group client proxy interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "msg-receiver-group", fallbackFactory = MsgReceiverGroupFallbackFactory::class)
interface IMsgReceiverGroupProxy : IMsgReceiverGroupApi
