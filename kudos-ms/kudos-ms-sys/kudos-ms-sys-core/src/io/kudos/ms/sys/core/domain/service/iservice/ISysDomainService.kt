package io.kudos.ms.sys.core.domain.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.common.domain.vo.response.SysDomainRow
import io.kudos.ms.sys.core.domain.model.po.SysDomain


/**
 * Domain service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDomainService : IBaseCrudService<String, SysDomain> {

    /**
     * Load a domain cache entry by domain name (only active=true is written to / hit in the by-name cache).
     *
     * @param domainName domain name, non-blank
     * @return cache entry; null if not found or inactive
     */
    fun getDomainFromCache(domainName: String): SysDomainCacheEntry?

    /**
     * Get the domain list for a tenant (direct DB query).
     *
     * @param tenantId tenant id
     */
    fun getDomainsByTenantId(tenantId: String): List<SysDomainRow>

    /**
     * Get the domain list for a system (direct DB query).
     *
     * @param systemCode system code
     */
    fun getDomainsBySystemCode(systemCode: String): List<SysDomainRow>

    /**
     * Update the enabled state and sync the cache.
     *
     * @param id domain id
     * @param active whether enabled
     * @return whether the update succeeded
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
