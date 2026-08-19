package io.kudos.ms.sys.client.accessrule.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.accessrule.proxy.ISysAccessRuleIpProxy
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import java.math.BigDecimal


/**
 * IP access rule fallback implementation.
 *
 * Note: [checkIpAccess] returns `false` (deny) when the remote is unreachable, which is a security-side default — better safe than sorry.
 *
 * @author K
 * @since 1.0.0
 */
open class SysAccessRuleIpFallback : AbstractHttpFallbackSupport("SysAccessRuleIpFallback"), ISysAccessRuleIpProxy {

    override fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow> {
        warnRead("getIpsByRuleId", ruleId)
        return emptyList()
    }

    override fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry> {
        warnRead("getIpsBySystemAndTenant", systemCode, tenantId)
        return emptyList()
    }

    override fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean {
        // Secure default: deny; if a caller needs to allow when sys is unreachable, implement a fail-open bypass on its own side.
        warnRead("checkIpAccess", ip, systemCode, tenantId)
        return false
    }

    override fun deleteByRuleId(ruleId: String): Int {
        errorWrite("deleteByRuleId", ruleId)
        return 0
    }
}
