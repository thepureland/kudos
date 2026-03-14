package io.kudos.ms.msg.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.msg.core.model.po.MsgReceive
import io.kudos.ms.msg.core.model.table.MsgReceives
import org.springframework.stereotype.Repository


/**
 * 消息接收数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class MsgReceiveDao : BaseCrudDao<String, MsgReceive, MsgReceives>() {



}
