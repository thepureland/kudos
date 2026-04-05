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
 * 只读 DAO，映射视图 `v_sys_access_rule_with_ip`。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class VSysAccessRuleWithIpDao : BaseReadOnlyDao<String, VSysAccessRuleWithIp, VSysAccessRuleWithIps>() {

    /**
     * 按父规则主键查询，结果映射为 [VSysAccessRuleWithIpRow]，按 `ipStart` 升序。
     */
    open fun searchByParentId(parentId: String): List<VSysAccessRuleWithIpRow> {
        val criteria = Criteria.and(VSysAccessRuleWithIp::parentId eq parentId)
        return searchAs(criteria, Order.asc("ipStart"))
    }

    /**
     * 按系统编码与租户维度查询；`tenantId == null` 时使用 `tenant_id IS NULL` 条件。
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
