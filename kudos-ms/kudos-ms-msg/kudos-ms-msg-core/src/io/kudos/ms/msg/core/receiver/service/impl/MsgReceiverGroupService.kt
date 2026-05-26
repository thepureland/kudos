package io.kudos.ms.msg.core.receiver.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import io.kudos.ms.msg.core.receiver.dao.MsgReceiverGroupDao
import io.kudos.ms.msg.core.receiver.model.po.MsgReceiverGroup
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiverGroupService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * Message receiver group business service.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class MsgReceiverGroupService(
    dao: MsgReceiverGroupDao
) : BaseCrudService<String, MsgReceiverGroup, MsgReceiverGroupDao>(dao), IMsgReceiverGroupService {

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == MsgReceiverGroupCacheEntry::class) dao.get(id, returnType)
        else super.get(id, returnType)

    @Transactional(readOnly = true)
    override fun getReceiverGroupById(id: String): MsgReceiverGroupCacheEntry? =
        get(id, MsgReceiverGroupCacheEntry::class)

    @Transactional(readOnly = true)
    override fun listActiveReceiverGroups(receiverGroupTypeDictCode: String?): List<MsgReceiverGroupCacheEntry> =
        dao.fetchActiveReceiverGroups(receiverGroupTypeDictCode)

}
