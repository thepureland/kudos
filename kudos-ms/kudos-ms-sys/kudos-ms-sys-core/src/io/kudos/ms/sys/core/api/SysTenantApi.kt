package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysTenantApi
import io.kudos.ms.sys.common.vo.tenant.SysTenantCacheEntry
import io.kudos.ms.sys.core.service.iservice.ISysTenantService
import org.springframework.stereotype.Component


/**
 * 租户 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysTenantApi(
    private val sysTenantService: ISysTenantService,
) : ISysTenantApi {

    override fun getTenantFromCache(id: String): SysTenantCacheEntry? = sysTenantService.getTenantFromCache(id)

    override fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> =
        sysTenantService.getTenantsFromCacheByIds(ids)

    override fun getTenantsBySubSystemCode(subSystemCode: String): List<SysTenantCacheEntry> =
        sysTenantService.getTenantsForSubSystemFromCache(subSystemCode).filter { it.active }
}
