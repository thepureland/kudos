package io.kudos.ms.sys.api.internal.controller.tenant

import io.kudos.ms.sys.common.tenant.api.ISysTenantResourceApi
import io.kudos.ms.sys.core.tenant.api.SysTenantResourceApi
import org.springframework.web.bind.annotation.RestController


/**
 * 租户-资源 关联 内部 RPC 控制器。路径继承自 [ISysTenantResourceApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysTenantResourceInternalController(
    private val sysTenantResourceApi: SysTenantResourceApi,
) : ISysTenantResourceApi {

    override fun getResourceIdsByTenantId(tenantId: String): Set<String> =
        sysTenantResourceApi.getResourceIdsByTenantId(tenantId)

    override fun getTenantIdsByResourceId(resourceId: String): Set<String> =
        sysTenantResourceApi.getTenantIdsByResourceId(resourceId)

    override fun batchBind(tenantId: String, resourceIds: Collection<String>): Int =
        sysTenantResourceApi.batchBind(tenantId, resourceIds)

    override fun unbind(tenantId: String, resourceId: String): Boolean =
        sysTenantResourceApi.unbind(tenantId, resourceId)

    override fun exists(tenantId: String, resourceId: String): Boolean =
        sysTenantResourceApi.exists(tenantId, resourceId)

}
