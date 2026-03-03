package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysTenantSystemApi
import io.kudos.ms.sys.core.service.iservice.ISysTenantSystemService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 租户-系统关系 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Service
open class SysTenantSystemApi : ISysTenantSystemApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysTenantSystemService: ISysTenantSystemService

    override fun searchSystemCodesByTenantId(tenantId: String): Set<String> {
        return sysTenantSystemService.searchSystemCodesByTenantId(tenantId)
    }

    override fun searchTenantIdsBySystemCode(systemCode: String): Set<String> {
        return sysTenantSystemService.searchTenantIdsBySystemCode(systemCode)
    }

    override fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> {
        return sysTenantSystemService.groupingSystemCodesByTenantIds(tenantIds)
    }

    override fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>?): Map<String, List<String>> {
        return sysTenantSystemService.groupingTenantIdsBySystemCodes(systemCodes)
    }

    override fun batchBind(tenantId: String, systemCodes: Collection<String>): Int {
        return sysTenantSystemService.batchBind(tenantId, systemCodes)
    }

    override fun unbind(tenantId: String, systemCode: String): Boolean {
        return sysTenantSystemService.unbind(tenantId, systemCode)
    }

    override fun exists(tenantId: String, systemCode: String): Boolean {
        return sysTenantSystemService.exists(tenantId, systemCode)
    }

    override fun deleteByTenantId(tenantId: String): Int {
        return sysTenantSystemService.deleteByTenantId(tenantId)
    }

    override fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int {
        return sysTenantSystemService.batchDeleteByTenantIds(tenantIds)
    }

    //endregion your codes 2

}
