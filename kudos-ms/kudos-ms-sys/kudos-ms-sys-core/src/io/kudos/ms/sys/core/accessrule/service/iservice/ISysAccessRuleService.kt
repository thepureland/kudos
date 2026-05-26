package io.kudos.ms.sys.core.accessrule.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule


/**
 * Access rule service interface.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAccessRuleService : IBaseCrudService<String, SysAccessRule> {


    /**
     * Get an access rule.
     *
     * @param systemCode system code, defaults to "default"
     * @param tenantId tenant id, defaults to "default"
     * @return access rule record, or null if not found
     * @author K
     * @since 1.0.0
     */
    fun getAccessRuleByTenantAndSystem(
        systemCode: String = "default",
        tenantId: String = "default"
    ): SysAccessRuleRow?

    /**
     * Get the access rule list for a tenant.
     *
     * @param tenantId tenant id
     * @return list of access rule records
     * @author K
     * @since 1.0.0
     */
    fun getAccessRulesByTenantId(tenantId: String): List<SysAccessRuleRow>

    /**
     * Get the access rule list for a system.
     *
     * @param systemCode system code
     * @return list of access rule records
     * @author K
     * @since 1.0.0
     */
    fun getAccessRulesBySystemCode(systemCode: String): List<SysAccessRuleRow>

    /**
     * Update the enabled state.
     *
     * @param id access rule id
     * @param active whether enabled
     * @return whether the update succeeded
     * @author K
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean


}
