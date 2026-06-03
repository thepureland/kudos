package io.kudos.ms.msg.core.send.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.msg.core.send.model.po.MsgSend
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Message send database table-entity binding object.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object MsgSends : StringIdTable<MsgSend>("msg_send") {

    /** Receiver group type dictionary code */
    var receiverGroupTypeDictCode = varchar("receiver_group_type_dict_code").bindTo { it.receiverGroupTypeDictCode }

    /** Receiver group ID */
    var receiverGroupId = varchar("receiver_group_id").bindTo { it.receiverGroupId }

    /** Message instance ID */
    var instanceId = varchar("instance_id").bindTo { it.instanceId }

    /** Message type dictionary code */
    var msgTypeDictCode = varchar("msg_type_dict_code").bindTo { it.msgTypeDictCode }

    /** Country-language dictionary code */
    var localeDictCode = varchar("locale_dict_code").bindTo { it.localeDictCode }

    /** Send status dictionary code */
    var sendStatusDictCode = varchar("send_status_dict_code").bindTo { it.sendStatusDictCode }

    /** Create time */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Update time */
    var updateTime = datetime("update_time").bindTo { it.updateTime }

    /** Send success count */
    var successCount = int("success_count").bindTo { it.successCount }

    /** Send fail count */
    var failCount = int("fail_count").bindTo { it.failCount }

    /** Scheduled job ID */
    var jobId = varchar("job_id").bindTo { it.jobId }

    /** Idempotency key — unique per tenant; identifies a business request so retries are deduplicated */
    var idempotencyKey = varchar("idempotency_key").bindTo { it.idempotencyKey }

    /** Tenant ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }




}
