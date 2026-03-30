package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysTenantApi
import io.kudos.ms.sys.common.vo.tenant.SysTenantCacheEntry
import io.kudos.ms.sys.core.service.iservice.ISysTenantService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 租户 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysTenantApi : ISysTenantApi {


    @Resource
    protected lateinit var sysTenantService: ISysTenantService

    override fun getTenantFromCache(id: String): SysTenantCacheEntry? {
        return sysTenantService.getTenantFromCache(id)
    }

    override fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> {
        return sysTenantService.getTenantsFromCacheByIds(ids)
    }

    override fun getTenantsBySubSystemCode(subSystemCode: String): List<SysTenantCacheEntry> {
        return sysTenantService.getTenantsForSubSystemFromCache(subSystemCode).filter { it.active }
    }


}
