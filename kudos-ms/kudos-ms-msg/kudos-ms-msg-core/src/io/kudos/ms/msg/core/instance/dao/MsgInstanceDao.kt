package io.kudos.ms.msg.core.instance.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.msg.core.instance.model.po.MsgInstance
import io.kudos.ms.msg.core.instance.model.table.MsgInstances
import org.springframework.stereotype.Repository


/**
 * 消息实例数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class MsgInstanceDao : BaseCrudDao<String, MsgInstance, MsgInstances>() {



}
