package io.kudos.ms.msg.core.receiver.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * 消息接收数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface MsgReceive : IDbEntity<String, MsgReceive> {

    companion object : DbEntityFactory<MsgReceive>()

    /** 接收者ID */
    var receiverId: String

    /** 发送ID */
    var sendId: String

    /** 接收状态字典码 */
    var receiveStatusDictCode: String

    /** 创建时间 */
    var createTime: LocalDateTime

    /** 更新时间 */
    var updateTime: LocalDateTime?

    /** 租户ID */
    var tenantId: String




}
