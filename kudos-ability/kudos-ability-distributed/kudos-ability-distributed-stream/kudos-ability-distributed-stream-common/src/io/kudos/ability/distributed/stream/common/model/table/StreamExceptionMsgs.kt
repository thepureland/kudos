package io.kudos.ability.distributed.stream.common.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ability.distributed.stream.common.model.po.StreamExceptionMsg
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

object StreamExceptionMsgs : StringIdTable<StreamExceptionMsg>("stream_exception_msgs") {

    var topic = varchar("topic").bindTo { it.topic }

    var msgHeaderJson = varchar("msg_header").bindTo { it.msgHeaderJson }

    var msgBodyJson = varchar("msg_body").bindTo { it.msgBodyJson }

    var createTime = datetime("create_time").bindTo { it.createTime }

}