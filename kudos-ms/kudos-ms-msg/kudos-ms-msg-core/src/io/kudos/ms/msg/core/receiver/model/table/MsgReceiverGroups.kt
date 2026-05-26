package io.kudos.ms.msg.core.receiver.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.msg.core.receiver.model.po.MsgReceiverGroup
import org.ktorm.schema.varchar


/**
 * Message receiver group database table-entity binding object.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object MsgReceiverGroups : ManagedTable<MsgReceiverGroup>("msg_receiver_group") {

    /** Receiver group type dictionary code */
    var receiverGroupTypeDictCode = varchar("receiver_group_type_dict_code").bindTo { it.receiverGroupTypeDictCode }

    /** Table where the group is defined */
    var defineTable = varchar("define_table").bindTo { it.defineTable }

    /** Field name of the group name in the specific group table */
    var nameColumn = varchar("name_column").bindTo { it.nameColumn }




}
