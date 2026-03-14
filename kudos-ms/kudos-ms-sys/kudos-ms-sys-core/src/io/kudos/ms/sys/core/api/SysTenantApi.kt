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
 * @since 1.0.0
 */
@Component
open class SysTenantApi : ISysTenantApi {


    @Resource
    protected lateinit var sysTenantService: ISysTenantService

    override fun getTenant(id: String): SysTenantCacheEntry? {
        return sysTenantService.getTenant(id)
    }

    override fun getTenantsBySubSystemCode(ids: Collection<String>): Map<String, SysTenantCacheEntry> {
        return sysTenantService.getTenantsBySubSystemCode(ids)
    }

    override fun getTenantsBySubSystemCode(subSystemCode: String): List<SysTenantCacheEntry> {
        return sysTenantService.getTenantsBySubSystemCode(subSystemCode)
    }


}