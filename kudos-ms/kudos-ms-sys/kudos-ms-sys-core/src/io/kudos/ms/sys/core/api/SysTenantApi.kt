package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysTenantApi
import io.kudos.ms.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ms.sys.core.service.iservice.ISysTenantService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 租户 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Component
open class SysTenantApi : ISysTenantApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysTenantService: ISysTenantService

    override fun getTenant(id: String): SysTenantCacheItem? {
        return sysTenantService.getTenant(id)
    }

    override fun getTenants(ids: Collection<String>): Map<String, SysTenantCacheItem> {
        return sysTenantService.getTenants(ids)
    }

    override fun getTenants(subSysDictCode: String): List<SysTenantCacheItem> {
        return sysTenantService.getTenants(subSysDictCode)
    }

    //endregion your codes 2

}