package io.kudos.ms.msg.client.instance.proxy

import io.kudos.ms.msg.client.instance.fallback.MsgInstanceFallbackFactory
import io.kudos.ms.msg.common.instance.api.IMsgInstanceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Message instance client proxy interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@FeignClient(name = "msg-instance", fallbackFactory = MsgInstanceFallbackFactory::class)
interface IMsgInstanceProxy : IMsgInstanceApi
