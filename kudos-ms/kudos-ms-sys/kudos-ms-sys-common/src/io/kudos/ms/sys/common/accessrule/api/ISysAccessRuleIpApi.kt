package io.kudos.ms.sys.common.accessrule.api

import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import java.math.BigDecimal


/**
 * ip访问规则 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAccessRuleIpApi {


    /**
     * 获取规则的IP列表
     *
     * @param ruleId 规则id
     * @return IP规则记录列表
     */
    fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow>

    /**
     * 根据系统编码和租户id从缓存获取IP规则列表
     *
     * @param systemCode 系统编码
     * @param tenantId 租户id，可以为null
     * @return IP规则缓存项列表
     */
    fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry>

    /**
     * 检查IP是否允许访问
     *
     * @param ip IP地址
     * @param systemCode 系统编码
     * @param tenantId 租户id，可以为null
     * @return 是否允许访问
     */
    fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean

    /**
     * 删除规则的所有IP
     *
     * @param ruleId 规则id
     * @return 删除的数量
     */
    fun deleteByRuleId(ruleId: String): Int


}
