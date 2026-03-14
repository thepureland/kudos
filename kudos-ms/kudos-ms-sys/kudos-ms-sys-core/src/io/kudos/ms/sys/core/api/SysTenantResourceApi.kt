package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysTenantResourceApi
import io.kudos.ms.sys.core.service.iservice.ISysTenantResourceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 租户-资源关系 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysTenantResourceApi : ISysTenantResourceApi {


    @Resource
    protected lateinit var sysTenantResourceService: ISysTenantResourceService

    override fun getResourceIdsByTenantId(tenantId: String): Set<String> {
        return sysTenantResourceService.getResourceIdsByTenantId(tenantId)
    }

    override fun getTenantIdsByResourceId(resourceId: String): Set<String> {
        return sysTenantResourceService.getTenantIdsByResourceId(resourceId)
    }

    override fun batchBind(tenantId: String, resourceIds: Collection<String>): Int {
        return sysTenantResourceService.batchBind(tenantId, resourceIds)
    }

    override fun unbind(tenantId: String, resourceId: String): Boolean {
        return sysTenantResourceService.unbind(tenantId, resourceId)
    }

    override fun exists(tenantId: String, resourceId: String): Boolean {
        return sysTenantResourceService.exists(tenantId, resourceId)
    }


}