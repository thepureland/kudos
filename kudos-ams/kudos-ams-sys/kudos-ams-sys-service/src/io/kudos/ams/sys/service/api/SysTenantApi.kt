package io.kudos.ams.sys.service.api

import io.kudos.ams.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ams.sys.service.biz.ibiz.ISysTenantBiz
import org.springframework.beans.factory.annotation.Autowired
import io.kudos.ams.sys.common.api.ISysTenantApi
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
    private lateinit var sysTenantBiz: ISysTenantBiz

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