package io.kudos.ms.sys.core.accessrule.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule
import org.ktorm.schema.varchar


/**
 * Access rule table-entity binding object.
 *
 * @author K
 * @since 1.0.0
 */
object SysAccessRules : ManagedTable<SysAccessRule>("sys_access_rule") {

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** System code */
    var systemCode = varchar("system_code").bindTo { it.systemCode }

    /** Access rule type dict code */
    var accessRuleTypeDictCode = varchar("access_rule_type_dict_code").bindTo { it.accessRuleTypeDictCode }




}
