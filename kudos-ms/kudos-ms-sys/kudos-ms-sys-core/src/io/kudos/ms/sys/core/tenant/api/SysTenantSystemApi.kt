package io.kudos.ms.sys.core.tenant.api

import io.kudos.ms.sys.common.tenant.api.ISysTenantSystemApi
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantSystemService
import org.springframework.stereotype.Service


/**
 * 租户-系统关系 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysTenantSystemApi(
    private val sysTenantSystemService: ISysTenantSystemService,
) : ISysTenantSystemApi {

    override fun searchSystemCodesByTenantId(tenantId: String): Set<String> =
        sysTenantSystemService.searchSystemCodesByTenantId(tenantId)

    override fun searchTenantIdsBySystemCode(systemCode: String): Set<String> =
        sysTenantSystemService.searchTenantIdsBySystemCode(systemCode)

    override fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> =
        sysTenantSystemService.groupingSystemCodesByTenantIds(tenantIds)

    override fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>?): Map<String, List<String>> =
        sysTenantSystemService.groupingTenantIdsBySystemCodes(systemCodes)

    override fun batchBind(tenantId: String, systemCodes: Collection<String>): Int =
        sysTenantSystemService.batchBind(tenantId, systemCodes)

    override fun unbind(tenantId: String, systemCode: String): Boolean =
        sysTenantSystemService.unbind(tenantId, systemCode)

    override fun exists(tenantId: String, systemCode: String): Boolean =
        sysTenantSystemService.exists(tenantId, systemCode)

    override fun deleteByTenantId(tenantId: String): Int = sysTenantSystemService.deleteByTenantId(tenantId)

    override fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int =
        sysTenantSystemService.batchDeleteByTenantIds(tenantIds)
}
