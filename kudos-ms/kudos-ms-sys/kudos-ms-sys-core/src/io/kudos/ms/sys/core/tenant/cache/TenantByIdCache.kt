package io.kudos.ms.sys.core.tenant.cache

import io.kudos.ability.cache.common.batch.keyvalue.BatchCacheable
import io.kudos.ability.cache.common.core.keyvalue.AbstractByIdCacheHandler
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.event.SysTenantBatchDeleted
import io.kudos.ms.sys.core.tenant.event.SysTenantDeleted
import io.kudos.ms.sys.core.tenant.event.SysTenantInserted
import io.kudos.ms.sys.core.tenant.event.SysTenantUpdated
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Tenant (by id) cache handler.
 *
 * 1. Source table: sys_tenant
 * 2. Caches all tenants, including those with active=false
 * 3. Cache key: id
 * 4. Cache value: SysTenantCacheEntry object
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class TenantByIdCache : AbstractByIdCacheHandler<String, SysTenantCacheEntry, SysTenantDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_TENANT_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysTenantCacheEntry? {
        return getSelf<TenantByIdCache>().getTenantById(key)
    }

    /**
     * Get tenant info from the cache by id. On miss, load from the DB and write back.
     *
     * @param id tenant id
     * @return SysTenantCacheEntry, or null if not found
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getTenantById(id: String): SysTenantCacheEntry? {
        return getById(id)
    }

    /**
     * Batch fetch tenant info from the cache by multiple ids. For misses, load from the DB and write back.
     *
     * @param ids collection of tenant ids
     * @return Map of tenant id -> SysTenantCacheEntry
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysTenantCacheEntry::class
    )
    open fun getTenantsByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> {
        return getByIds(ids)
    }

    /**
     * Sync after tenant insert (overload taking business object and id). Behaves like the single-arg [syncOnInsert].
     *
     * @param any business object, used only to differentiate the overload
     * @param id tenant id
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * Sync after tenant update (overload taking business object). Behaves like the single-arg [syncOnUpdate].
     */
    open fun syncOnUpdate(any: Any, id: String) {
        syncOnUpdate(id)
    }

    // region Event subscriptions (dispatched by SysTenantService after transaction commit) ---------------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysTenantInserted): Unit = syncOnInsert(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysTenantUpdated): Unit = syncOnUpdate(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysTenantDeleted): Unit = syncOnDelete(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysTenantBatchDeleted): Unit = syncOnBatchDelete(event.ids)

    // endregion

    override fun itemDesc() = "tenant"

}