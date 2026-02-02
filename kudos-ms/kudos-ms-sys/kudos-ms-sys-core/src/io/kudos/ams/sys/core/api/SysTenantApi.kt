package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ms.sys.core.service.iservice.ISysTenantService
import org.springframework.beans.factory.annotation.Autowired
import io.kudos.ms.sys.common.api.ISysTenantApi
import org.springframework.stereotype.Service


/**
 * 租户 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Service
open class SysTenantApi : ISysTenantApi {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var sysTenantBiz: ISysTenantService

    override fun getTenant(id: String): SysTenantCacheItem? {
        return sysTenantBiz.getTenant(id)
    }

    override fun getTenants(ids: Collection<String>): Map<String, SysTenantCacheItem> {
        return sysTenantBiz.getTenants(ids)
    }

    override fun getTenants(subSysDictCode: String): List<SysTenantCacheItem> {
        return sysTenantBiz.getTenants(subSysDictCode)
    }

    //endregion your codes 2

}