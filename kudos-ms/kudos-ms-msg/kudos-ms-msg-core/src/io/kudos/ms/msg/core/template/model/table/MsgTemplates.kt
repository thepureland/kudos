package io.kudos.ms.msg.core.template.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.msg.core.template.model.po.MsgTemplate
import org.ktorm.schema.boolean
import org.ktorm.schema.varchar


/**
 * Message template database table-entity binding object.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object MsgTemplates : StringIdTable<MsgTemplate>("msg_template") {

    /** Send type dictionary code */
    var sendTypeDictCode = varchar("send_type_dict_code").bindTo { it.sendTypeDictCode }

    /** Event type dictionary code */
    var eventTypeDictCode = varchar("event_type_dict_code").bindTo { it.eventTypeDictCode }

    /** Message type dictionary code */
    var msgTypeDictCode = varchar("msg_type_dict_code").bindTo { it.msgTypeDictCode }

    /** Template group code */
    var receiverGroupCode = varchar("receiver_group_code").bindTo { it.receiverGroupCode }

    /** Country-language dictionary code */
    var localeDictCode = varchar("locale_dict_code").bindTo { it.localeDictCode }

    /** Template title */
    var title = varchar("title").bindTo { it.title }

    /** Template content */
    var content = varchar("content").bindTo { it.content }

    /** Whether default values are enabled */
    var defaultActive = boolean("default_active").bindTo { it.defaultActive }

    /** Default template title */
    var defaultTitle = varchar("default_title").bindTo { it.defaultTitle }

    /** Default template content */
    var defaultContent = varchar("default_content").bindTo { it.defaultContent }

    /** Tenant ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }




}
