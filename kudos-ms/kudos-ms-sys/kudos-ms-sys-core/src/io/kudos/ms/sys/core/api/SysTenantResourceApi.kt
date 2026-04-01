package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysTenantResourceApi
import io.kudos.ms.sys.core.service.iservice.ISysTenantResourceService
import org.springframework.stereotype.Service


/**
 * 租户-资源关系 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysTenantResourceApi(
    private val sysTenantResourceService: ISysTenantResourceService,
) : ISysTenantResourceApi {

    override fun getResourceIdsByTenantId(tenantId: String): Set<String> =
        sysTenantResourceService.getResourceIdsByTenantId(tenantId)

    override fun getTenantIdsByResourceId(resourceId: String): Set<String> =
        sysTenantResourceService.getTenantIdsByResourceId(resourceId)

    override fun batchBind(tenantId: String, resourceIds: Collection<String>): Int =
        sysTenantResourceService.batchBind(tenantId, resourceIds)

    override fun unbind(tenantId: String, resourceId: String): Boolean =
        sysTenantResourceService.unbind(tenantId, resourceId)

    override fun exists(tenantId: String, resourceId: String): Boolean =
        sysTenantResourceService.exists(tenantId, resourceId)
}
