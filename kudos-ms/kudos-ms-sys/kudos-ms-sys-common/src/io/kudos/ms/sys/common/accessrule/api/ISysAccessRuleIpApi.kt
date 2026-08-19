package io.kudos.ms.sys.common.accessrule.api

import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import org.springframework.web.service.annotation.DeleteExchange
import org.springframework.web.service.annotation.GetExchange
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
    @GetExchange("/api/internal/sys/accessRuleIp/getIpsByRuleId")
    fun getIpsByRuleId(@RequestParam ruleId: String): List<SysAccessRuleIpRow>

    /**
     * Gets the IP rule list from cache by system code and tenant id.
     */
    @GetExchange("/api/internal/sys/accessRuleIp/getIpsBySystemAndTenant")
    fun getIpsBySystemAndTenant(
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String?
    ): List<SysAccessRuleIpCacheEntry>

    /**
     * Checks whether an IP is allowed to access (`true` = allow, `false` = deny).
     *
     * Hit on a blacklist rule denies; when whitelist rules exist for the dimension,
     * only a whitelist hit allows; with no effective rule configured, access is allowed by default.
     */
    @GetExchange("/api/internal/sys/accessRuleIp/checkIpAccess")
    fun checkIpAccess(
        @RequestParam ip: BigDecimal,
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String?
    ): Boolean

    /**
     * Deletes all IPs of a rule.
     */
    @DeleteExchange("/api/internal/sys/accessRuleIp/deleteByRuleId")
    fun deleteByRuleId(@RequestParam ruleId: String): Int


}
