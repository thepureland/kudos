package io.kudos.ms.sys.common.accessrule.api

import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.math.BigDecimal


/**
 * IP access rule external API.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAccessRuleIpApi {


    /**
     * Gets the list of IPs for a rule.
     */
    @GetMapping("/api/internal/sys/accessRuleIp/getIpsByRuleId")
    fun getIpsByRuleId(@RequestParam ruleId: String): List<SysAccessRuleIpRow>

    /**
     * Gets the IP rule list from cache by system code and tenant id.
     */
    @GetMapping("/api/internal/sys/accessRuleIp/getIpsBySystemAndTenant")
    fun getIpsBySystemAndTenant(
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String?
    ): List<SysAccessRuleIpCacheEntry>

    /**
     * Checks whether an IP is allowed to access.
     */
    @GetMapping("/api/internal/sys/accessRuleIp/checkIpAccess")
    fun checkIpAccess(
        @RequestParam ip: BigDecimal,
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String?
    ): Boolean

    /**
     * Deletes all IPs of a rule.
     */
    @DeleteMapping("/api/internal/sys/accessRuleIp/deleteByRuleId")
    fun deleteByRuleId(@RequestParam ruleId: String): Int


}
