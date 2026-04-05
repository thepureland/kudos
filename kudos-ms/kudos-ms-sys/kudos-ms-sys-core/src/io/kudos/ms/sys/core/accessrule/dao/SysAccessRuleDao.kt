package io.kudos.ms.sys.core.accessrule.dao
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule
import io.kudos.ms.sys.core.accessrule.model.table.SysAccessRules
import org.springframework.stereotype.Repository


/**
 * 访问规则数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysAccessRuleDao : BaseCrudDao<String, SysAccessRule, SysAccessRules>() {



}