package io.kudos.ms.msg.core.receiver.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.msg.core.receiver.model.po.MsgUnreceived
import io.kudos.ms.msg.core.receiver.model.table.MsgUnreceiveds
import org.springframework.stereotype.Repository


/**
 * 未送达消息数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class MsgUnreceivedDao : BaseCrudDao<String, MsgUnreceived, MsgUnreceiveds>()
