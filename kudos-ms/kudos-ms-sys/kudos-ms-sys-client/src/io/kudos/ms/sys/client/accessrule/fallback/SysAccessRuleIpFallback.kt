package io.kudos.ms.sys.client.accessrule.fallback

import io.kudos.ms.sys.client.accessrule.proxy.ISysAccessRuleIpProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import org.springframework.stereotype.Component
import java.math.BigDecimal


/**
 * IP 访问规则 Feign 容错降级实现。
 *
 * 注意：[checkIpAccess] 在远端不可达时返回 `false`（拒绝），属于安全侧默认 —— 宁可错杀也不放过。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysAccessRuleIpFallback : SysClientFallbackSupport("SysAccessRuleIpFallback"), ISysAccessRuleIpProxy {

    override fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow> {
        warnRead("getIpsByRuleId", ruleId)
        return emptyList()
    }

    override fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry> {
        warnRead("getIpsBySystemAndTenant", systemCode, tenantId)
        return emptyList()
    }

    override fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean {
        // 安全默认：拒绝；若调用方需要在 sys 不可达时放行，请在自己侧做 fail-open 旁路。
        warnRead("checkIpAccess", ip, systemCode, tenantId)
        return false
    }

    override fun deleteByRuleId(ruleId: String): Int {
        errorWrite("deleteByRuleId", ruleId)
        return 0
    }
}
