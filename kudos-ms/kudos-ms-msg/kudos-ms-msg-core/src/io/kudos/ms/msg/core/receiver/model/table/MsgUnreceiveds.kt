package io.kudos.ms.msg.core.receiver.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.msg.core.receiver.model.po.MsgUnreceived
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Undelivered message database table-entity binding object.
 *
 * @author K
 * @since 1.0.0
 */
object MsgUnreceiveds : StringIdTable<MsgUnreceived>("msg_unreceived") {

    var receiverId = varchar("receiver_id").bindTo { it.receiverId }

    var sendId = varchar("send_id").bindTo { it.sendId }

    var publishMethodDictCode = varchar("publish_method_dict_code").bindTo { it.publishMethodDictCode }

    var failReason = varchar("fail_reason").bindTo { it.failReason }

    var retryCount = int("retry_count").bindTo { it.retryCount }

    var lastRetryTime = datetime("last_retry_time").bindTo { it.lastRetryTime }

    var resolved = boolean("resolved").bindTo { it.resolved }

    var createTime = datetime("create_time").bindTo { it.createTime }

    var updateTime = datetime("update_time").bindTo { it.updateTime }

    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

}
