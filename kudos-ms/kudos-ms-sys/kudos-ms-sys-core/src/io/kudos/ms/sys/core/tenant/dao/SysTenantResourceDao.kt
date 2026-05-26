package io.kudos.ms.sys.core.tenant.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.tenant.model.po.SysTenantResource
import io.kudos.ms.sys.core.tenant.model.table.SysTenantResources
import org.springframework.stereotype.Repository


/**
 * Tenant-resource relationship data access object.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class SysTenantResourceDao : BaseCrudDao<String, SysTenantResource, SysTenantResources>() {


    /**
     * Find the corresponding resource ids by tenant id.
     *
     * @param tenantId tenant id
     * @return Set of resource ids
     */
    fun searchResourceIdsByTenantId(tenantId: String): Set<String> {
        val criteria = Criteria(SysTenantResource::tenantId eq tenantId)
        return searchProperty(criteria, SysTenantResource::resourceId).toSet()
    }

    /**
     * Find the corresponding tenant ids by resource id.
     *
     * @param resourceId resource id
     * @return Set of tenant ids
     */
    fun searchTenantIdsByResourceId(resourceId: String): Set<String> {
        val criteria = Criteria(SysTenantResource::resourceId eq resourceId)
        return searchProperty(criteria, SysTenantResource::tenantId).toSet()
    }

    /**
     * Check whether the relationship exists.
     *
     * @param tenantId tenant id
     * @param resourceId resource id
     * @return whether it exists
     */
    fun exists(tenantId: String, resourceId: String): Boolean {
        val criteria = Criteria.and(
            SysTenantResource::tenantId eq tenantId,
            SysTenantResource::resourceId eq resourceId
        )
        return count(criteria) > 0
    }

    /**
     * Delete the relationship by tenant id and resource id.
     *
     * @param tenantId tenant id
     * @param resourceId resource id
     * @return number of deleted rows
     */
    fun deleteByTenantIdAndResourceId(tenantId: String, resourceId: String): Int {
        val criteria = Criteria.and(
            SysTenantResource::tenantId eq tenantId,
            SysTenantResource::resourceId eq resourceId
        )
        return batchDeleteCriteria(criteria)
    }


}