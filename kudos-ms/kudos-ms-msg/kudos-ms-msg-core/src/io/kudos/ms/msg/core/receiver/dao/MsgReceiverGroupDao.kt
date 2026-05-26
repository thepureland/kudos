package io.kudos.ms.msg.core.receiver.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import io.kudos.ms.msg.core.receiver.model.po.MsgReceiverGroup
import io.kudos.ms.msg.core.receiver.model.table.MsgReceiverGroups
import org.springframework.stereotype.Repository


/**
 * Message receiver group data access object.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class MsgReceiverGroupDao : BaseCrudDao<String, MsgReceiverGroup, MsgReceiverGroups>() {

    open fun fetchActiveReceiverGroups(receiverGroupTypeDictCode: String?): List<MsgReceiverGroupCacheEntry> {
        val criteria = if (receiverGroupTypeDictCode.isNullOrBlank()) {
            Criteria(MsgReceiverGroup::active eq true)
        } else {
            Criteria.and(
                MsgReceiverGroup::active eq true,
                MsgReceiverGroup::receiverGroupTypeDictCode eq receiverGroupTypeDictCode,
            )
        }
        return searchAs(criteria)
    }

}
