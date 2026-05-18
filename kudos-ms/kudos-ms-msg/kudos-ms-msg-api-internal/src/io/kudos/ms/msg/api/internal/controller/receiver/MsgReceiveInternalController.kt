package io.kudos.ms.msg.api.internal.controller.receiver

import io.kudos.ms.msg.common.receiver.api.IMsgReceiveApi
import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import io.kudos.ms.msg.core.receiver.api.MsgReceiveApi
import org.springframework.web.bind.annotation.RestController


/**
 * 消息接收 内部 RPC 控制器。路径继承自 [IMsgReceiveApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class MsgReceiveInternalController(
    private val msgReceiveApi: MsgReceiveApi,
) : IMsgReceiveApi {

    override fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry> =
        msgReceiveApi.getReceivesByUserId(receiverId)

    override fun getUnreadCountByUserId(receiverId: String): Int =
        msgReceiveApi.getUnreadCountByUserId(receiverId)

    override fun markRead(id: String): Boolean =
        msgReceiveApi.markRead(id)

    override fun markAllReadByUserId(receiverId: String): Int =
        msgReceiveApi.markAllReadByUserId(receiverId)

}
