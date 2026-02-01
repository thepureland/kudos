package io.kudos.ams.msg.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.msg.core.model.po.MsgTemplate
import org.ktorm.schema.*


/**
 * 消息模板数据库表-实体关联对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
object MsgTemplates : StringIdTable<MsgTemplate>("msg_template") {
//endregion your codes 1

    /** 发送类型字典码 */
    var sendTypeDictCode = varchar("send_type_dict_code").bindTo { it.sendTypeDictCode }

    /** 事件类型字典码 */
    var eventTypeDictCode = varchar("event_type_dict_code").bindTo { it.eventTypeDictCode }

    /** 消息类型字典码 */
    var msgTypeDictCode = varchar("msg_type_dict_code").bindTo { it.msgTypeDictCode }

    /** 模板分组编码 */
    var receiverGroupCode = varchar("receiver_group_code").bindTo { it.receiverGroupCode }

    /** 国家-语言字典码 */
    var localeDictCode = varchar("locale_dict_code").bindTo { it.localeDictCode }

    /** 模板标题 */
    var title = varchar("title").bindTo { it.title }

    /** 模板内容 */
    var content = varchar("content").bindTo { it.content }

    /** 是否启用默认值 */
    var defaultActive = boolean("default_active").bindTo { it.defaultActive }

    /** 模板标题默认值 */
    var defaultTitle = varchar("default_title").bindTo { it.defaultTitle }

    /** 模板内容默认值 */
    var defaultContent = varchar("default_content").bindTo { it.defaultContent }

    /** 租户ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }


    //region your codes 2

    //endregion your codes 2

}
