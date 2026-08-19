package io.kudos.ms.sys.client.tenant.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.tenant.proxy.ISysTenantResourceProxy


/**
 * Tenant-resource relationship fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
open class SysTenantResourceFallback : AbstractHttpFallbackSupport("SysTenantResourceFallback"), ISysTenantResourceProxy {

    override fun getResourceIdsByTenantId(tenantId: String): Set<String> {
        warnRead("getResourceIdsByTenantId", tenantId)
        return emptySet()
    }

    override fun getTenantIdsByResourceId(resourceId: String): Set<String> {
        warnRead("getTenantIdsByResourceId", resourceId)
        return emptySet()
    }

    override fun batchBind(tenantId: String, resourceIds: Collection<String>): Int {
        errorWrite("batchBind", tenantId, resourceIds)
        return 0
    }

    override fun unbind(tenantId: String, resourceId: String): Boolean {
        errorWrite("unbind", tenantId, resourceId)
        return false
    }

    override fun exists(tenantId: String, resourceId: String): Boolean {
        // Safe default: treat as "unauthorized" when remote is unreachable
        warnRead("exists", tenantId, resourceId)
        return false
    }
}
