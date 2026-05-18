package io.kudos.ms.msg.core.receiver.api

import io.kudos.ms.msg.common.receiver.api.IMsgReceiveApi
import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import jakarta.annotation.Resource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * 消息接收API本地实现。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Primary
@Service
open class MsgReceiveApi : IMsgReceiveApi {

    @Resource
    private lateinit var msgReceiveService: IMsgReceiveService

    override fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry> =
        msgReceiveService.getReceivesByUserId(receiverId)

    override fun getUnreadCountByUserId(receiverId: String): Int =
        msgReceiveService.getUnreadCountByUserId(receiverId)

    override fun markRead(id: String): Boolean =
        msgReceiveService.markRead(id)

    override fun markAllReadByUserId(receiverId: String): Int =
        msgReceiveService.markAllReadByUserId(receiverId)

}
