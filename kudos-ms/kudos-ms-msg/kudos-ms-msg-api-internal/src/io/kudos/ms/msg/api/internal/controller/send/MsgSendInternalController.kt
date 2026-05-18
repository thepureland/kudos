package io.kudos.ms.msg.api.internal.controller.send

import io.kudos.ms.msg.common.send.api.IMsgSendApi
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import io.kudos.ms.msg.core.send.api.MsgSendApi
import org.springframework.web.bind.annotation.RestController


/**
 * 消息发送 内部 RPC 控制器。路径继承自 [IMsgSendApi] 方法级注解。
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
