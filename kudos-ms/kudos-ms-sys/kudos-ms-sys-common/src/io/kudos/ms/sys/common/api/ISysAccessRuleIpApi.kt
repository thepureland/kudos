package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpCacheItem
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpPayload
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpRecord


/**
 * ip访问规则 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysAccessRuleIpApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 获取规则的IP列表
     *
     * @param ruleId 规则id
     * @return IP规则记录列表
     */
    fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRecord>

    /**
     * 根据系统编码和租户id从缓存获取IP规则列表
     *
     * @param systemCode 系统编码
     * @param tenantId 租户id，可以为null
     * @return IP规则缓存项列表
     */
    fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheItem>

    /**
     * 检查IP是否允许访问
     *
     * @param ip IP地址（long类型）
     * @param systemCode 系统编码
     * @param tenantId 租户id，可以为null
     * @return 是否允许访问
     */
    fun checkIpAccess(ip: Long, systemCode: String, tenantId: String?): Boolean

    /**
     * 批量保存或更新IP规则
     *
     * @param ruleId 规则id
     * @param ips IP规则载体列表
     * @return 成功保存或更新的数量
     */
    fun batchSaveOrUpdate(ruleId: String, ips: List<SysAccessRuleIpPayload>): Int

    /**
     * 删除规则的所有IP
     *
     * @param ruleId 规则id
     * @return 删除的数量
     */
    fun deleteByRuleId(ruleId: String): Int

    //endregion your codes 2

}