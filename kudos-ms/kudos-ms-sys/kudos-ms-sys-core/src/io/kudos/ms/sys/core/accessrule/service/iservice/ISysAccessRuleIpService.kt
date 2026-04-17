package io.kudos.ms.sys.core.accessrule.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp
import java.math.BigDecimal


/**
 * ip访问规则业务接口
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAccessRuleIpService : IBaseCrudService<String, SysAccessRuleIp> {


    /**
     * 获取规则的IP列表
     *
     * @param ruleId 规则id
     * @return IP规则记录列表
     * @author K
     * @since 1.0.0
     */
    fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow>

    /**
     * 根据系统编码和租户id从缓存获取IP规则列表
     *
     * @param systemCode 系统编码
     * @param tenantId 租户id，可以为null
     * @return IP规则缓存项列表
     * @author K
     * @since 1.0.0
     */
    fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry>

    /**
     * 检查 IP 是否允许访问（按缓存中规则做 32 位无符号区间包含判断）。
     *
     * **限制**：仅匹配可把 `ip_start`/`ip_end` 当作无符号 32 位上下界的规则；字典为 ipv6 的条目不参与本方法（需扩展 API 再支持）。
     *
     * @param ip ip的BigDecimal表示
     * @param systemCode 系统编码
     * @param tenantId 租户 id，可以为 null（平台级）
     * @return 是否落在任一未过期且启用的规则内
     * @author K
     * @since 1.0.0
     */
    fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean

    /**
     * 删除规则的所有IP
     *
     * @param ruleId 规则id
     * @return 删除的数量
     * @author K
     * @since 1.0.0
     */
    fun deleteByRuleId(ruleId: String): Int

    /**
     * 仅更新单条 IP 访问规则的启用标记；成功后按父规则维度刷新 IP 规则缓存。
     *
     * @param id IP 规则主键（`sys_access_rule_ip.id`）
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
