package io.kudos.ms.sys.core.tenant.service.iservice

import io.kudos.base.model.vo.IdAndName
import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantRow
import io.kudos.ms.sys.core.tenant.model.po.SysTenant


/**
 * Tenant service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysTenantService : IBaseCrudService<String, SysTenant> {

    /**
     * Load tenant cache entry by id and cache the result (includes inactive).
     */
    fun getTenantFromCache(id: String): SysTenantCacheEntry?

    /**
     * Batch load tenant cache entries by id set (includes inactive).
     */
    fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry>

    /**
     * Resolve tenant list from cache by sub-system code (includes inactive; binding relationships sourced from the tenant-system Hash cache).
     */
    fun getTenantsForSubSystemFromCache(subSystemCode: String): List<SysTenantCacheEntry>

    /**
     * Load all tenants from database as cache entry type (matches the DB; used for full-list scenarios, not a Spring Cache full scan).
     */
    fun getAllTenantsFromCache(): List<SysTenantCacheEntry>

    /**
     * Update active flag and sync cache.
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * Get tenant list row by id (direct DB query, not cached).
     */
    fun getTenantRecord(id: String): SysTenantRow?

    /**
     * Get tenant record by name.
     */
    fun getTenantByName(name: String): SysTenantRow?

    /**
     * Get the set of sub-system codes bound to the tenant from cache.
     */
    fun getSubSystemCodesFromCache(tenantId: String): Set<String>

    /**
     * Get id/name projections of all active tenants under the given sub-system from cache.
     *
     * @param subSystemCode Sub-system code
     * @return List of id and name (only active=true)
     */
    fun getActiveTenantIdAndNamesForSubSystem(subSystemCode: String): List<IdAndName<String>>


}
