package io.kudos.ms.sys.client.tenant.fallback

import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.client.tenant.proxy.ISysTenantProxy
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import org.springframework.stereotype.Component


/**
 * Tenant Feign fallback implementation.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysTenantFallback : SysClientFallbackSupport("SysTenantFallback"), ISysTenantProxy {

    override fun getTenantFromCache(id: String): SysTenantCacheEntry? {
        warnRead("getTenantFromCache", id)
        return null
    }

    override fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> {
        warnRead("getTenantsFromCacheByIds", ids)
        return emptyMap()
    }

    override fun getTenantsBySubSystemCode(subSystemCode: String): List<SysTenantCacheEntry> {
        warnRead("getTenantsBySubSystemCode", subSystemCode)
        return emptyList()
    }
}
