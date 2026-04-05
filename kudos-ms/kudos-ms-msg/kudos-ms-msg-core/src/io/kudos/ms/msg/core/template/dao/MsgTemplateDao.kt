package io.kudos.ms.msg.core.template.dao
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.msg.core.template.model.po.MsgTemplate
import io.kudos.ms.msg.core.template.model.table.MsgTemplates
import org.springframework.stereotype.Repository


/**
 * 消息模板数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class MsgTemplateDao : BaseCrudDao<String, MsgTemplate, MsgTemplates>() {



}
