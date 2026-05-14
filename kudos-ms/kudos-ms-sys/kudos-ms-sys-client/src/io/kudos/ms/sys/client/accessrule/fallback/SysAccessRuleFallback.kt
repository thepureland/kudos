package io.kudos.ms.sys.client.accessrule.fallback

import io.kudos.ms.sys.client.accessrule.proxy.ISysAccessRuleProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import org.springframework.stereotype.Component


/**
 * 访问规则 Feign 容错降级实现。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysAccessRuleFallback : SysClientFallbackSupport("SysAccessRuleFallback"), ISysAccessRuleProxy {

    override fun getAccessRuleByTenantAndSystem(
        systemCode: String,
        tenantId: String,
    ): SysAccessRuleRow? {
        warnRead("getAccessRuleByTenantAndSystem", systemCode, tenantId)
        return null
    }

    override fun updateActive(id: String, active: Boolean): Boolean {
        errorWrite("updateActive", id, active)
        return false
    }
}
