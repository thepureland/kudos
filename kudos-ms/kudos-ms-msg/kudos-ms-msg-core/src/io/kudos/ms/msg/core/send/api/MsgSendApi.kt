package io.kudos.ms.msg.core.send.api

import io.kudos.ms.msg.common.send.api.IMsgSendApi
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import io.kudos.ms.msg.core.send.service.iservice.IMsgPublishService
import jakarta.annotation.Resource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * Local implementation of the message send API.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Primary
@Service
open class MsgSendApi : IMsgSendApi {

    @Resource
    private lateinit var msgPublishService: IMsgPublishService

    override fun publish(request: MsgPublishRequest): String? =
        msgPublishService.publish(request)

}
