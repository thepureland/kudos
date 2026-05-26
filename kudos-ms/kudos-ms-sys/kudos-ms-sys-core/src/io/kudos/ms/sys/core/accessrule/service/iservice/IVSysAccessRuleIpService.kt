package io.kudos.ms.sys.core.accessrule.service.iservice

import io.kudos.base.support.service.iservice.IBaseReadOnlyService
import io.kudos.ms.sys.common.accessrule.vo.response.VSysAccessRuleWithIpRow
import io.kudos.ms.sys.core.accessrule.model.po.VSysAccessRuleWithIp

/**
 * Read-only service interface for the view `v_sys_access_rule_with_ip` (parent rule LEFT JOIN child IP table).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IVSysAccessRuleIpService : IBaseReadOnlyService<String, VSysAccessRuleWithIp> {

    /**
     * Query view rows by parent access rule primary key.
     *
     * @param parentId `sys_access_rule.id`
     */
    fun searchByParentId(parentId: String): List<VSysAccessRuleWithIpRow>

    /**
     * Filter view rows by system code and tenant; `tenantId == null` means the parent rule has `tenant_id IS NULL`.
     *
     * @param systemCode system code
     * @param tenantId tenant id; null means the platform tenant
     */
    fun searchBySystemCodeAndTenantId(systemCode: String, tenantId: String?): List<VSysAccessRuleWithIpRow>
}
