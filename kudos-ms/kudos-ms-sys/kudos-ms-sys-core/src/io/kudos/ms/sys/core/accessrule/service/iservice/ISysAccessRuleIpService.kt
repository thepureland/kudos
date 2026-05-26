package io.kudos.ms.sys.core.accessrule.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp
import java.math.BigDecimal


/**
 * IP access rule service interface.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAccessRuleIpService : IBaseCrudService<String, SysAccessRuleIp> {


    /**
     * Get the IP list for a rule.
     *
     * @param ruleId rule id
     * @return list of IP rule records
     * @author K
     * @since 1.0.0
     */
    fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow>

    /**
     * Fetch the IP rule list from cache by system code and tenant id.
     *
     * @param systemCode system code
     * @param tenantId tenant id, may be null
     * @return list of IP rule cache entries
     * @author K
     * @since 1.0.0
     */
    fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry>

    /**
     * Check whether an IP is allowed (treats cached `ip_start`/`ip_end` as an unsigned 32-bit interval).
     *
     * **Limitation**: only rules whose `ip_start`/`ip_end` fit into unsigned 32-bit bounds participate;
     * ipv6-dict entries are not handled by this method (extend the API to add support).
     *
     * @param ip BigDecimal representation of the IP
     * @param systemCode system code
     * @param tenantId tenant id, may be null (platform-level)
     * @return whether the IP falls within any non-expired, active rule
     * @author K
     * @since 1.0.0
     */
    fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean

    /**
     * Delete all IPs for a rule.
     *
     * @param ruleId rule id
     * @return number of rows deleted
     * @author K
     * @since 1.0.0
     */
    fun deleteByRuleId(ruleId: String): Int

    /**
     * Update only the enabled flag of a single IP access rule; on success, refreshes the IP rule cache at the parent-rule dimension.
     *
     * @param id IP rule primary key (`sys_access_rule_ip.id`)
     * @param active whether enabled
     * @return whether the update succeeded
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
