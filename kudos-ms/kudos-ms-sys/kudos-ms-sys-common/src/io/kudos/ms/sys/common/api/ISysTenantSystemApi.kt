package io.kudos.ms.sys.common.api


/**
 * 租户-系统关系 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantSystemApi {


    fun searchSystemCodesByTenantId(tenantId: String): Set<String>

    fun searchTenantIdsBySystemCode(systemCode: String): Set<String>

    fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>? = null): Map<String, List<String>>

    fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>? = null): Map<String, List<String>>

    fun batchBind(tenantId: String, systemCodes: Collection<String>): Int

    fun unbind(tenantId: String, systemCode: String): Boolean

    fun exists(tenantId: String, systemCode: String): Boolean

    fun deleteByTenantId(tenantId: String): Int

    fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int


}