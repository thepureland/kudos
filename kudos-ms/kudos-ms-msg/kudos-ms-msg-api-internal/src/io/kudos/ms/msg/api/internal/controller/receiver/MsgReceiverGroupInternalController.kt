package io.kudos.ms.msg.api.internal.controller.receiver

import io.kudos.ms.msg.common.receiver.api.IMsgReceiverGroupApi
import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import io.kudos.ms.msg.core.receiver.api.MsgReceiverGroupApi
import org.springframework.web.bind.annotation.RestController


/**
 * Internal RPC controller for receiver groups.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@RestController
class MsgReceiverGroupInternalController(
    private val msgReceiverGroupApi: MsgReceiverGroupApi
) : IMsgReceiverGroupApi {

    override fun getReceiverGroupById(id: String): MsgReceiverGroupCacheEntry? =
        msgReceiverGroupApi.getReceiverGroupById(id)

    override fun listActiveReceiverGroups(receiverGroupTypeDictCode: String?): List<MsgReceiverGroupCacheEntry> =
        msgReceiverGroupApi.listActiveReceiverGroups(receiverGroupTypeDictCode)

}
