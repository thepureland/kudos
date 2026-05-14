package io.kudos.ms.sys.client.tenant.fallback

import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.client.tenant.proxy.ISysTenantLocaleProxy
import org.springframework.stereotype.Component


/**
 * 租户-语言关系 Feign 容错降级实现。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysTenantLocaleFallback : SysClientFallbackSupport("SysTenantLocaleFallback"), ISysTenantLocaleProxy {

    override fun getLocaleCodesByTenantId(tenantId: String): Set<String> {
        warnRead("getLocaleCodesByTenantId", tenantId)
        return emptySet()
    }

    override fun getTenantIdsByLocaleCode(localeCode: String): Set<String> {
        warnRead("getTenantIdsByLocaleCode", localeCode)
        return emptySet()
    }

    override fun batchBind(tenantId: String, localeCodes: Collection<String>): Int {
        errorWrite("batchBind", tenantId, localeCodes)
        return 0
    }

    override fun unbind(tenantId: String, localeCode: String): Boolean {
        errorWrite("unbind", tenantId, localeCode)
        return false
    }

    override fun exists(tenantId: String, localeCode: String): Boolean {
        warnRead("exists", tenantId, localeCode)
        return false
    }
}
