package io.kudos.ability.distributed.stream.common.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * stream异常消息实体
 */
interface SysMqFailMsg : IDbEntity<String, SysMqFailMsg> {

    companion object Companion : DbEntityFactory<SysMqFailMsg>()

    /**
     * 消息主题
     */
    var topic: String

    /**
     * 消息头json串
     */
    var msgHeaderJson: String

    /**
     * 消息体json串
     */
    var msgBodyJson: String

    /**
     * 创建时间
     */
    var createTime: LocalDateTime
}