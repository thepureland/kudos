package io.kudos.ms.msg.core.receiver.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * 未送达消息的失败追踪记录。
 *
 * 与 [MsgReceive] 互补：成功送达写 [MsgReceive]，失败的接收人写本表。
 * 重试由 admin 或后续 retry scheduler 触发，成功后置 [resolved] = true。
 *
 * @author K
 * @since 1.0.0
 */
interface MsgUnreceived : IDbEntity<String, MsgUnreceived> {

    companion object : DbEntityFactory<MsgUnreceived>()

    /** 原本应该收到消息的用户ID */
    var receiverId: String

    /** 关联的发送批次ID（msg_send.id） */
    var sendId: String

    /** 失败发生的渠道（publish_method 字典码） */
    var publishMethodDictCode: String

    /** 失败原因，文本，参考 [io.kudos.ms.msg.common.receiver.enums.MsgUnreceivedReasonEnum] */
    var failReason: String?

    /** 已重试次数 */
    var retryCount: Int

    /** 最近一次重试的时间；null 表示还没重试过 */
    var lastRetryTime: LocalDateTime?

    /** 是否已处理（重试成功 / admin 关闭后置 true） */
    var resolved: Boolean

    /** 创建时间 */
    var createTime: LocalDateTime

    /** 更新时间 */
    var updateTime: LocalDateTime?

    /** 租户ID */
    var tenantId: String

}
