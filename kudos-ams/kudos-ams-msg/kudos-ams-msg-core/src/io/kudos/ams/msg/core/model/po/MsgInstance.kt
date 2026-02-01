package io.kudos.ams.msg.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * 消息实例数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface MsgInstance : IDbEntity<String, MsgInstance> {
//endregion your codes 1

    companion object : DbEntityFactory<MsgInstance>()

    /** 国家-语言字典码 */
    var localeDictCode: String?

    /** 标题 */
    var title: String?

    /** 通知内容 */
    var content: String?

    /** 消息模板id */
    var templateId: String?

    /** 发送类型字典码 */
    var sendTypeDictCode: String?

    /** 事件类型字典码 */
    var eventTypeDictCode: String?

    /** 消息类型字典码 */
    var msgTypeDictCode: String?

    /** 有效期起 */
    var validTimeStart: LocalDateTime

    /** 有效期止 */
    var validTimeEnd: LocalDateTime

    /** 租户ID */
    var tenantId: String


    //region your codes 2

    //endregion your codes 2

}
