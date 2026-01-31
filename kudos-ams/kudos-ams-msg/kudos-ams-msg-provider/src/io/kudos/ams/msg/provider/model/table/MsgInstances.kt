package io.kudos.ams.msg.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.msg.provider.model.po.MsgInstance
import org.ktorm.schema.*


/**
 * 消息实例数据库表-实体关联对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
object MsgInstances : StringIdTable<MsgInstance>("msg_instance") {
//endregion your codes 1

    /** 国家-语言字典码 */
    var localeDictCode = varchar("locale_dict_code").bindTo { it.localeDictCode }

    /** 标题 */
    var title = varchar("title").bindTo { it.title }

    /** 通知内容 */
    var content = varchar("content").bindTo { it.content }

    /** 消息模板id */
    var templateId = varchar("template_id").bindTo { it.templateId }

    /** 发送类型字典码 */
    var sendTypeDictCode = varchar("send_type_dict_code").bindTo { it.sendTypeDictCode }

    /** 事件类型字典码 */
    var eventTypeDictCode = varchar("event_type_dict_code").bindTo { it.eventTypeDictCode }

    /** 消息类型字典码 */
    var msgTypeDictCode = varchar("msg_type_dict_code").bindTo { it.msgTypeDictCode }

    /** 有效期起 */
    var validTimeStart = datetime("valid_time_start").bindTo { it.validTimeStart }

    /** 有效期止 */
    var validTimeEnd = datetime("valid_time_end").bindTo { it.validTimeEnd }

    /** 租户ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }


    //region your codes 2

    //endregion your codes 2

}
