package io.kudos.ms.sys.common.accessrule.api

import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
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
     */
    @GetMapping("/api/internal/sys/accessRuleIp/getIpsByRuleId")
    fun getIpsByRuleId(@RequestParam ruleId: String): List<SysAccessRuleIpRow>

    /**
     * 根据系统编码和租户id从缓存获取IP规则列表
     */
    @GetMapping("/api/internal/sys/accessRuleIp/getIpsBySystemAndTenant")
    fun getIpsBySystemAndTenant(
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String?
    ): List<SysAccessRuleIpCacheEntry>

    /**
     * 检查IP是否允许访问
     */
    @GetMapping("/api/internal/sys/accessRuleIp/checkIpAccess")
    fun checkIpAccess(
        @RequestParam ip: BigDecimal,
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String?
    ): Boolean

    /**
     * 删除规则的所有IP
     */
    @DeleteMapping("/api/internal/sys/accessRuleIp/deleteByRuleId")
    fun deleteByRuleId(@RequestParam ruleId: String): Int


}
