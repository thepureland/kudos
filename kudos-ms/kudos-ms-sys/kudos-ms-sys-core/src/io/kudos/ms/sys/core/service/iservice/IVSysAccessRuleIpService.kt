package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.service.iservice.IBaseReadOnlyService
import io.kudos.ms.sys.common.vo.accessrule.response.VSysAccessRuleWithIpRow
import io.kudos.ms.sys.core.model.po.VSysAccessRuleWithIp

/**
 * 视图 `v_sys_access_rule_with_ip`（父规则 LEFT JOIN IP 子表）的只读服务接口。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IVSysAccessRuleIpService : IBaseReadOnlyService<String, VSysAccessRuleWithIp> {

    /**
     * 按父访问规则主键查询视图行列表。
     *
     * @param parentId `sys_access_rule.id`
     */
    fun searchByParentId(parentId: String): List<VSysAccessRuleWithIpRow>

    /**
     * 按系统编码与租户筛选视图行；`tenantId == null` 时表示父规则 `tenant_id IS NULL`。
     *
     * @param systemCode 系统编码
     * @param tenantId 租户 id，null 表示平台租户
     */
    fun searchBySystemCodeAndTenantId(systemCode: String, tenantId: String?): List<VSysAccessRuleWithIpRow>
}
