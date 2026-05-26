package io.kudos.ms.sys.api.internal.controller.tenant

import io.kudos.ms.sys.common.tenant.api.ISysTenantApi
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.core.tenant.api.SysTenantApi
import org.springframework.web.bind.annotation.RestController


/**
 * Tenant internal RPC controller. Paths are inherited from method-level annotations on [ISysTenantApi].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysTenantInternalController(
    private val sysTenantApi: SysTenantApi,
) : ISysTenantApi {

    override fun getTenantFromCache(id: String): SysTenantCacheEntry? =
        sysTenantApi.getTenantFromCache(id)

    override fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> =
        sysTenantApi.getTenantsFromCacheByIds(ids)

    override fun getTenantsBySubSystemCode(subSystemCode: String): List<SysTenantCacheEntry> =
        sysTenantApi.getTenantsBySubSystemCode(subSystemCode)

}
