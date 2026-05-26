package io.kudos.ms.msg.core.receiver.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import io.kudos.ms.msg.core.receiver.model.po.MsgReceiverGroup


/**
 * Message receiver group business service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgReceiverGroupService : IBaseCrudService<String, MsgReceiverGroup> {

    fun getReceiverGroupById(id: String): MsgReceiverGroupCacheEntry?

    fun listActiveReceiverGroups(receiverGroupTypeDictCode: String?): List<MsgReceiverGroupCacheEntry>

}
