package io.kudos.ms.msg.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity


/**
 * 消息接收者群组数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface MsgReceiverGroup : IManagedDbEntity<String, MsgReceiverGroup> {

    companion object : DbEntityFactory<MsgReceiverGroup>()

    /** 接收者群组类型字典码 */
    var receiverGroupTypeDictCode: String

    /** 群组定义的表 */
    var defineTable: String

    /** 群组名称在具体群组表中的字段名 */
    var nameColumn: String




}
