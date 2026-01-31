package io.kudos.ams.msg.provider.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ams.msg.provider.model.po.MsgTemplate
import io.kudos.ams.msg.provider.model.table.MsgTemplates
import org.springframework.stereotype.Repository


/**
 * 消息模板数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class MsgTemplateDao : BaseCrudDao<String, MsgTemplate, MsgTemplates>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
