package io.kudos.ms.msg.core.send.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * Message send database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface MsgSend : IDbEntity<String, MsgSend> {

    companion object : DbEntityFactory<MsgSend>()

    /** Receiver group type dictionary code */
    var receiverGroupTypeDictCode: String

    /** Receiver group ID */
    var receiverGroupId: String?

    /** Message instance ID */
    var instanceId: String

    /** Message type dictionary code */
    var msgTypeDictCode: String

    /** Country-language dictionary code */
    var localeDictCode: String?

    /** Send status dictionary code */
    var sendStatusDictCode: String

    /** Create time */
    var createTime: LocalDateTime

    /** Update time */
    var updateTime: LocalDateTime?

    /** Send success count */
    var successCount: Int?

    /** Send fail count */
    var failCount: Int?

    /** Scheduled job ID */
    var jobId: String?

    /** Idempotency key — unique per tenant; identifies a business request so retries are deduplicated */
    var idempotencyKey: String?

    /** Tenant ID */
    var tenantId: String




}
