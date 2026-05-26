package io.kudos.ms.msg.core.receiver.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * Failure tracking record for undelivered messages.
 *
 * Complementary to [MsgReceive]: successful deliveries are written to [MsgReceive],
 * while failed receivers are written to this table.
 * Retries are triggered by admin or a subsequent retry scheduler, and [resolved] is set to true on success.
 *
 * @author K
 * @since 1.0.0
 */
interface MsgUnreceived : IDbEntity<String, MsgUnreceived> {

    companion object : DbEntityFactory<MsgUnreceived>()

    /** ID of the user who should have received the message */
    var receiverId: String

    /** Associated send batch ID (msg_send.id) */
    var sendId: String

    /** Channel on which the failure occurred (publish_method dictionary code) */
    var publishMethodDictCode: String

    /** Failure reason, text, see [io.kudos.ms.msg.common.receiver.enums.MsgUnreceivedReasonEnum] */
    var failReason: String?

    /** Retry count */
    var retryCount: Int

    /** Time of the most recent retry; null means no retry has occurred yet */
    var lastRetryTime: LocalDateTime?

    /** Whether it has been resolved (set to true after a successful retry / admin close) */
    var resolved: Boolean

    /** Create time */
    var createTime: LocalDateTime

    /** Update time */
    var updateTime: LocalDateTime?

    /** Tenant ID */
    var tenantId: String

}
