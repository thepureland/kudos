package io.kudos.ms.msg.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity


/**
 * 消息模板数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface MsgTemplate : IDbEntity<String, MsgTemplate> {
//endregion your codes 1

    companion object : DbEntityFactory<MsgTemplate>()

    /** 发送类型字典码 */
    var sendTypeDictCode: String

    /** 事件类型字典码 */
    var eventTypeDictCode: String

    /** 消息类型字典码 */
    var msgTypeDictCode: String

    /** 模板分组编码 */
    var receiverGroupCode: String?

    /** 国家-语言字典码 */
    var localeDictCode: String?

    /** 模板标题 */
    var title: String?

    /** 模板内容 */
    var content: String?

    /** 是否启用默认值 */
    var defaultActive: Boolean

    /** 模板标题默认值 */
    var defaultTitle: String?

    /** 模板内容默认值 */
    var defaultContent: String?

    /** 租户ID */
    var tenantId: String


    //region your codes 2

    //endregion your codes 2

}
