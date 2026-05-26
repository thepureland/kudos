package io.kudos.ms.sys.core.accessrule.dao
import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.sys.common.accessrule.vo.response.VSysAccessRuleWithIpRow
import io.kudos.ms.sys.core.accessrule.model.po.VSysAccessRuleWithIp
import io.kudos.ms.sys.core.accessrule.model.table.VSysAccessRuleWithIps
import org.springframework.stereotype.Repository

/**
 * Read-only DAO mapped to the view `v_sys_access_rule_with_ip`.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class VSysAccessRuleWithIpDao : BaseReadOnlyDao<String, VSysAccessRuleWithIp, VSysAccessRuleWithIps>() {

    /**
     * Query by parent rule primary key; results map to [VSysAccessRuleWithIpRow] ordered by `ipStart` ascending.
     */
    open fun searchByParentId(parentId: String): List<VSysAccessRuleWithIpRow> {
        val criteria = Criteria.and(VSysAccessRuleWithIp::parentId eq parentId)
        return searchAs(criteria, Order.asc("ipStart"))
    }

    /**
     * Query by system code and tenant dimension; uses `tenant_id IS NULL` when `tenantId == null`.
     */
    open fun searchBySystemCodeAndTenantId(systemCode: String, tenantId: String?): List<VSysAccessRuleWithIpRow> {
        val tenantCriterion = if (tenantId == null) {
            Criterion(VSysAccessRuleWithIp::tenantId.name, OperatorEnum.IS_NULL, null)
        } else {
            VSysAccessRuleWithIp::tenantId eq tenantId
        }
        val criteria = Criteria.and(
            VSysAccessRuleWithIp::systemCode eq systemCode,
            tenantCriterion,
        )
        return searchAs(criteria, Order.asc("parentId"), Order.asc("ipStart"))
    }
}
