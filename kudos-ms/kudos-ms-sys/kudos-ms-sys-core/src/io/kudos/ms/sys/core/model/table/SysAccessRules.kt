package io.kudos.ms.sys.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.model.po.SysAccessRule
import org.ktorm.schema.varchar


/**
 * 访问规则数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
object SysAccessRules : ManagedTable<SysAccessRule>("sys_access_rule") {

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 系统编码 */
    var systemCode = varchar("system_code").bindTo { it.systemCode }

    /** 规则类型字典代码 */
    var ruleTypeDictCode = varchar("rule_type_dict_code").bindTo { it.ruleTypeDictCode }




}
