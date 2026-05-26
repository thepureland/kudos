package io.kudos.ms.msg.core.send.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.msg.core.send.model.po.MsgSend
import io.kudos.ms.msg.core.send.model.table.MsgSends
import org.springframework.stereotype.Repository


/**
 * Message send data access object.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class MsgSendDao : BaseCrudDao<String, MsgSend, MsgSends>() {



}
