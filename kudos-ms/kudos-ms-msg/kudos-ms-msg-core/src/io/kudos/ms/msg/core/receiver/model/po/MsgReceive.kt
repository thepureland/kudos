package io.kudos.ms.msg.core.receiver.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * Message receive database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface MsgReceive : IDbEntity<String, MsgReceive> {

    companion object : DbEntityFactory<MsgReceive>()

    /** Receiver ID */
    var receiverId: String

    /** Send ID */
    var sendId: String

    /** Receive status dictionary code */
    var receiveStatusDictCode: String

    /** Create time */
    var createTime: LocalDateTime

    /** Update time */
    var updateTime: LocalDateTime?

    /** Tenant ID */
    var tenantId: String




}
