package io.kudos.ms.sys.common.tenant.api

import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * Tenant external API.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysTenantApi {

    /**
     * Returns the tenant with the given id (from cache, including inactive).
     */
    @GetExchange("/api/internal/sys/tenant/getTenant")
    fun getTenantFromCache(@RequestParam id: String): SysTenantCacheEntry?

    /**
     * Returns tenant info for the given set of ids and caches the results (including inactive).
     */
    @PostExchange("/api/internal/sys/tenant/getTenantsByIds")
    fun getTenantsFromCacheByIds(@RequestBody ids: Collection<String>): Map<String, SysTenantCacheEntry>

    /**
     * Returns tenants under the given sub-system (active only; internally fetches sub-system-associated tenants and filters by `active`).
     */
    @GetExchange("/api/internal/sys/tenant/getTenantsBySubSystemCode")
    fun getTenantsBySubSystemCode(@RequestParam subSystemCode: String): List<SysTenantCacheEntry>


}
