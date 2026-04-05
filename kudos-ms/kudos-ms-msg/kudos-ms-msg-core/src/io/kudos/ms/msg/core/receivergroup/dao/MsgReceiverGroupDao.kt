package io.kudos.ms.msg.core.receivergroup.dao
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.msg.core.receivergroup.model.po.MsgReceiverGroup
import io.kudos.ms.msg.core.receivergroup.model.table.MsgReceiverGroups
import org.springframework.stereotype.Repository


/**
 * 消息接收者群组数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class MsgReceiverGroupDao : BaseCrudDao<String, MsgReceiverGroup, MsgReceiverGroups>() {



}
