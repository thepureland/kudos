package io.kudos.ms.msg.core.receiver.api

import io.kudos.ms.msg.common.receiver.api.IMsgReceiverGroupApi
import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiverGroupService
import jakarta.annotation.Resource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * Local implementation of the message receiver group API.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Primary
@Service
open class MsgReceiverGroupApi : IMsgReceiverGroupApi {

    @Resource
    private lateinit var msgReceiverGroupService: IMsgReceiverGroupService

    override fun getReceiverGroupById(id: String): MsgReceiverGroupCacheEntry? =
        msgReceiverGroupService.getReceiverGroupById(id)

    override fun listActiveReceiverGroups(receiverGroupTypeDictCode: String?): List<MsgReceiverGroupCacheEntry> =
        msgReceiverGroupService.listActiveReceiverGroups(receiverGroupTypeDictCode)

}
