package io.kudos.ms.sys.client.accessrule.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.accessrule.proxy.ISysAccessRuleProxy
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow


/**
 * Access rule fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
open class SysAccessRuleFallback : AbstractHttpFallbackSupport("SysAccessRuleFallback"), ISysAccessRuleProxy {

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
