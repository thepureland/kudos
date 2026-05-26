package io.kudos.ms.msg.core.instance.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.msg.core.instance.model.po.MsgInstance
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Message instance database table-entity binding object.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object MsgInstances : StringIdTable<MsgInstance>("msg_instance") {

    /** Country-language dictionary code */
    var localeDictCode = varchar("locale_dict_code").bindTo { it.localeDictCode }

    /** Title */
    var title = varchar("title").bindTo { it.title }

    /** Notification content */
    var content = varchar("content").bindTo { it.content }

    /** Message template id */
    var templateId = varchar("template_id").bindTo { it.templateId }

    /** Send type dictionary code */
    var sendTypeDictCode = varchar("send_type_dict_code").bindTo { it.sendTypeDictCode }

    /** Event type dictionary code */
    var eventTypeDictCode = varchar("event_type_dict_code").bindTo { it.eventTypeDictCode }

    /** Message type dictionary code */
    var msgTypeDictCode = varchar("msg_type_dict_code").bindTo { it.msgTypeDictCode }

    /** Valid period start */
    var validTimeStart = datetime("valid_time_start").bindTo { it.validTimeStart }

    /** Valid period end */
    var validTimeEnd = datetime("valid_time_end").bindTo { it.validTimeEnd }

    /** Tenant ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }




}
