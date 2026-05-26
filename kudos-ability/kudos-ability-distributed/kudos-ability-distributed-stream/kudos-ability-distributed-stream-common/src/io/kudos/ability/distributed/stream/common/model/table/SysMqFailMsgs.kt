package io.kudos.ability.distributed.stream.common.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ability.distributed.stream.common.model.po.SysMqFailMsg
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/**
 * Message queue failed message table.
 * Persists send-failed messages to support subsequent retry processing.
 */
object SysMqFailMsgs : StringIdTable<SysMqFailMsg>("sys_mq_fail_msg") {

    var topic = varchar("topic").bindTo { it.topic }

    var msgHeaderJson = varchar("msg_header_json").bindTo { it.msgHeaderJson }

    var msgBodyJson = varchar("msg_body_json").bindTo { it.msgBodyJson }

    var createTime = datetime("create_time").bindTo { it.createTime }

}