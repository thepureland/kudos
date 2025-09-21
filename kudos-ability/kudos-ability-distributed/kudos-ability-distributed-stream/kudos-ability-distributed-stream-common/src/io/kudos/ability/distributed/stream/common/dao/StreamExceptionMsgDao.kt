package io.kudos.ability.distributed.stream.common.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import io.kudos.ability.distributed.stream.common.model.table.SysMqFailMsgs

/**
 * stream异常消息数据访问对象
 */
open class StreamExceptionMsgDao: BaseCrudDao<String, SysMqFailMsg, SysMqFailMsgs>()
