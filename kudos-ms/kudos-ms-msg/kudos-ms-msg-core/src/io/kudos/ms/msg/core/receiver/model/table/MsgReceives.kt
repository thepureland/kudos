package io.kudos.ms.msg.core.receiver.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.msg.core.receiver.model.po.MsgReceive
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Message receive database table-entity binding object.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object MsgReceives : StringIdTable<MsgReceive>("msg_receive") {

    /** Receiver ID */
    var receiverId = varchar("receiver_id").bindTo { it.receiverId }

    /** Send ID */
    var sendId = varchar("send_id").bindTo { it.sendId }

    /** Receive status dictionary code */
    var receiveStatusDictCode = varchar("receive_status_dict_code").bindTo { it.receiveStatusDictCode }

    /** Create time */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Update time */
    var updateTime = datetime("update_time").bindTo { it.updateTime }

    /** Tenant ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }




}
