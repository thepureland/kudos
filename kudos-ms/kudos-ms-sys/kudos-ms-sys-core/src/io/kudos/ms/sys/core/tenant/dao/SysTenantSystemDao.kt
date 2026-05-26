package io.kudos.ms.sys.core.tenant.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.sys.common.tenant.vo.SysTenantSystemCacheEntry
import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem
import io.kudos.ms.sys.core.tenant.model.table.SysTenantSystems
import org.springframework.stereotype.Repository


/**
 * Tenant-system relationship data access object.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class SysTenantSystemDao : BaseCrudDao<String, SysTenantSystem, SysTenantSystems>() {


    /**
     * Find the corresponding system codes by tenant id.
     *
     * @param tenantId tenant id
     * @return Set of system codes
     */
    fun searchSystemCodesByTenantId(tenantId: String): Set<String> =
        searchProperty(Criteria(SysTenantSystem::tenantId eq tenantId), SysTenantSystem::systemCode).toSet()

    /**
     * Find the corresponding tenant ids by system code.
     *
     * @param systemCode system code
     * @return Set of tenant ids
     */
    fun searchTenantIdsBySystemCode(systemCode: String): Set<String> =
        searchProperty(Criteria(SysTenantSystem::systemCode eq systemCode), SysTenantSystem::tenantId).toSet()

    /**
     * Group system codes by tenant id.
     *
     * @param tenantIds query condition: collection of tenant ids; when null, all records are returned; defaults to null
     * @return Map of tenant id -> List of system codes
     */
    fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>? = null): Map<String, List<String>> =
        groupRows(
            keyProp = SysTenantSystem::tenantId,
            valueProp = SysTenantSystem::systemCode,
            filter = tenantIds?.let { Criteria(SysTenantSystem::tenantId inList it) },
        )

    /**
     * Group tenant ids by system code.
     *
     * @param systemCodes query condition: collection of system codes; when null, all records are returned; defaults to null
     * @return Map of system code -> List of tenant ids
     */
    fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>? = null): Map<String, List<String>> =
        groupRows(
            keyProp = SysTenantSystem::systemCode,
            valueProp = SysTenantSystem::tenantId,
            filter = systemCodes?.let { Criteria(SysTenantSystem::systemCode inList it) },
        )

    private fun groupRows(
        keyProp: kotlin.reflect.KProperty1<SysTenantSystem, *>,
        valueProp: kotlin.reflect.KProperty1<SysTenantSystem, *>,
        filter: Criteria?,
    ): Map<String, List<String>> {
        val returnProperties = listOf(keyProp, valueProp)
        val results = if (filter == null) allSearchProperties(returnProperties)
            else searchProperties(filter, returnProperties)
        return results
            .mapNotNull { row ->
                val k = row[keyProp.name] as? String
                val v = row[valueProp.name] as? String
                if (k != null && v != null) k to v else null
            }
            .groupBy({ it.first }, { it.second })
    }

    /**
     * Check whether the relationship exists.
     *
     * @param tenantId tenant id
     * @param systemCode system code
     * @return whether it exists
     * @author AI: Cursor
     */
    fun exists(tenantId: String, systemCode: String): Boolean = count(
        Criteria.and(SysTenantSystem::tenantId eq tenantId, SysTenantSystem::systemCode eq systemCode)
    ) > 0

    /**
     * Delete the relationship by tenant id and system code.
     *
     * @param tenantId tenant id
     * @param systemCode system code
     * @return number of deleted rows
     */
    fun deleteByTenantIdAndSystemCode(tenantId: String, systemCode: String): Int = batchDeleteCriteria(
        Criteria.and(SysTenantSystem::tenantId eq tenantId, SysTenantSystem::systemCode eq systemCode)
    )

    /**
     * Batch delete relationships by a collection of tenant ids.
     *
     * @param tenantIds collection of tenant ids
     * @return number of deleted rows
     */
    fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int {
        if (tenantIds.isEmpty()) return 0
        return batchDeleteCriteria(Criteria(SysTenantSystem::tenantId inList tenantIds))
    }

    /**
     * Full query for loading the Hash cache.
     *
     * @return all tenant-system relationship cache items
     */
    open fun fetchAllForCache(): List<SysTenantSystemCacheEntry> =
        searchAs<SysTenantSystemCacheEntry>(null)

    /**
     * Query by system code for the Hash cache to write back by secondary property.
     *
     * @param systemCode system code
     * @return list of tenant-system relationship cache items under this system
     */
    open fun fetchCacheItemsBySystemCode(systemCode: String): List<SysTenantSystemCacheEntry> =
        searchAs<SysTenantSystemCacheEntry>(Criteria(SysTenantSystem::systemCode eq systemCode))

    /**
     * Query by tenant id for the Hash cache to write back by secondary property.
     *
     * @param tenantId tenant id
     * @return list of tenant-system relationship cache items under this tenant
     */
    open fun fetchCacheItemsByTenantId(tenantId: String): List<SysTenantSystemCacheEntry> =
        searchAs<SysTenantSystemCacheEntry>(Criteria(SysTenantSystem::tenantId eq tenantId))


}
