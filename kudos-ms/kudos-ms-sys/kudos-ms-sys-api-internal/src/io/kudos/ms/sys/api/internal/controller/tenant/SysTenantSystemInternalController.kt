package io.kudos.ms.sys.api.internal.controller.tenant

import io.kudos.ms.sys.common.tenant.api.ISysTenantSystemApi
import io.kudos.ms.sys.core.tenant.api.SysTenantSystemApi
import org.springframework.web.bind.annotation.RestController


/**
 * Tenant-system association internal RPC controller. Paths are inherited from method-level annotations on [ISysTenantSystemApi].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysTenantSystemInternalController(
    private val sysTenantSystemApi: SysTenantSystemApi,
) : ISysTenantSystemApi {

    override fun searchSystemCodesByTenantId(tenantId: String): Set<String> =
        sysTenantSystemApi.searchSystemCodesByTenantId(tenantId)

    override fun searchTenantIdsBySystemCode(systemCode: String): Set<String> =
        sysTenantSystemApi.searchTenantIdsBySystemCode(systemCode)

    override fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> =
        sysTenantSystemApi.groupingSystemCodesByTenantIds(tenantIds)

    override fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>?): Map<String, List<String>> =
        sysTenantSystemApi.groupingTenantIdsBySystemCodes(systemCodes)

    override fun batchBind(tenantId: String, systemCodes: Collection<String>): Int =
        sysTenantSystemApi.batchBind(tenantId, systemCodes)

    override fun unbind(tenantId: String, systemCode: String): Boolean =
        sysTenantSystemApi.unbind(tenantId, systemCode)

    override fun exists(tenantId: String, systemCode: String): Boolean =
        sysTenantSystemApi.exists(tenantId, systemCode)

    override fun deleteByTenantId(tenantId: String): Int =
        sysTenantSystemApi.deleteByTenantId(tenantId)

    override fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int =
        sysTenantSystemApi.batchDeleteByTenantIds(tenantIds)

}
