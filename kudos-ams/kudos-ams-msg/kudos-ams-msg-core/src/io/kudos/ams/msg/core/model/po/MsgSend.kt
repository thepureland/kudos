package io.kudos.ams.msg.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * 消息发送数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface MsgSend : IDbEntity<String, MsgSend> {
//endregion your codes 1

    companion object : DbEntityFactory<MsgSend>()

    /** 接收者群组类型字典码 */
    var receiverGroupTypeDictCode: String

    /** 接收者群组ID */
    var receiverGroupId: String?

    /** 消息实例ID */
    var instanceId: String

    /** 消息类型字典码 */
    var msgTypeDictCode: String

    /** 国家-语言字典码 */
    var localeDictCode: String?

    /** 发送状态字典码 */
    var sendStatusDictCode: String

    /** 创建时间 */
    var createTime: LocalDateTime

    /** 更新时间 */
    var updateTime: LocalDateTime?

    /** 发送成功数量 */
    var successCount: Int?

    /** 发送失败数量 */
    var failCount: Int?

    /** 定时任务ID */
    var jobId: String?

    /** 租户ID */
    var tenantId: String


    //region your codes 2

    //endregion your codes 2

}
