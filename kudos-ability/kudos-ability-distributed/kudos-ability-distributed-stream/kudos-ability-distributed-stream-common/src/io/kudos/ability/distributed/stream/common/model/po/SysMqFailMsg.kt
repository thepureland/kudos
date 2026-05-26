package io.kudos.ability.distributed.stream.common.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Stream exception message entity.
 */
interface SysMqFailMsg : IDbEntity<String, SysMqFailMsg> {

    companion object Companion : DbEntityFactory<SysMqFailMsg>()

    /**
     * Message topic.
     */
    var topic: String

    /**
     * Message header JSON string.
     */
    var msgHeaderJson: String

    /**
     * Message body JSON string.
     */
    var msgBodyJson: String

    /**
     * Creation time.
     */
    var createTime: LocalDateTime
}