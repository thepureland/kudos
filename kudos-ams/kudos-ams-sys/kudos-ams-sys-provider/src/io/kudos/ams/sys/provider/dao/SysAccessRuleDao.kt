package io.kudos.ams.sys.provider.dao

import io.kudos.ams.sys.provider.model.po.SysAccessRule
import io.kudos.ams.sys.provider.model.table.SysAccessRules
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 访问规则数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysAccessRuleDao : BaseCrudDao<String, SysAccessRule, SysAccessRules>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}