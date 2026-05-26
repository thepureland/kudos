package io.kudos.ms.msg.core.receiver.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity


/**
 * Message receiver group database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface MsgReceiverGroup : IManagedDbEntity<String, MsgReceiverGroup> {

    companion object : DbEntityFactory<MsgReceiverGroup>()

    /** Receiver group type dictionary code */
    var receiverGroupTypeDictCode: String

    /** Table where the group is defined */
    var defineTable: String

    /** Field name of the group name in the specific group table */
    var nameColumn: String




}
