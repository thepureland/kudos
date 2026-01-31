package io.kudos.ams.msg.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable
import io.kudos.ams.msg.provider.model.po.MsgReceiverGroup
import org.ktorm.schema.varchar


/**
 * 消息接收者群组数据库表-实体关联对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
object MsgReceiverGroups : MaintainableTable<MsgReceiverGroup>("msg_receiver_group") {
//endregion your codes 1

    /** 接收者群组类型字典码 */
    var receiverGroupTypeDictCode = varchar("receiver_group_type_dict_code").bindTo { it.receiverGroupTypeDictCode }

    /** 群组定义的表 */
    var defineTable = varchar("define_table").bindTo { it.defineTable }

    /** 群组名称在具体群组表中的字段名 */
    var nameColumn = varchar("name_column").bindTo { it.nameColumn }


    //region your codes 2

    //endregion your codes 2

}
