package io.kudos.ability.distributed.stream.common.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ability.distributed.stream.common.model.po.StreamExceptionMsg
import io.kudos.ability.distributed.stream.common.model.table.StreamExceptionMsgs

/**
 * stream异常消息数据访问对象
 */
open class StreamExceptionMsgDao: BaseCrudDao<String, StreamExceptionMsg, StreamExceptionMsgs>()
