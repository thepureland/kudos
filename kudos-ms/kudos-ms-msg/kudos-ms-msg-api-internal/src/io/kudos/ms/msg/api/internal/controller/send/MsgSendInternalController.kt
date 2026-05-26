package io.kudos.ms.msg.api.internal.controller.send

import io.kudos.ms.msg.common.send.api.IMsgSendApi
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import io.kudos.ms.msg.core.send.api.MsgSendApi
import org.springframework.web.bind.annotation.RestController


/**
 * Internal RPC controller for message send. Paths are inherited from [IMsgSendApi] method-level annotations.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class MsgSendInternalController(
    private val msgSendApi: MsgSendApi,
) : IMsgSendApi {

    override fun publish(request: MsgPublishRequest): String? =
        msgSendApi.publish(request)

}
