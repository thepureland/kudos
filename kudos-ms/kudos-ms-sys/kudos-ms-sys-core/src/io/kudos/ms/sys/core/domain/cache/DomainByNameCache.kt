package io.kudos.ms.sys.core.domain.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.core.domain.dao.SysDomainDao
import io.kudos.ms.sys.core.domain.event.SysDomainBatchDeleted
import io.kudos.ms.sys.core.domain.event.SysDomainDeleted
import io.kudos.ms.sys.core.domain.event.SysDomainInserted
import io.kudos.ms.sys.core.domain.event.SysDomainUpdated
import io.kudos.ms.sys.core.domain.model.po.SysDomain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Domain cache handler.
 *
 * 1. Source table: sys_domain
 * 2. Caches domains excluding those with active=false
 * 3. Cache key: domain name
 * 4. Cache value: SysDomainCacheEntry instance
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class DomainByNameCache : AbstractKeyValueCacheHandler<SysDomainCacheEntry>() {

    @Autowired
    private lateinit var dao: SysDomainDao

    companion object {
        private const val CACHE_NAME = "SYS_DOMAIN_BY_NAME"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): SysDomainCacheEntry? {
        return getSelf<DomainByNameCache>().getDomain(key)
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is disabled; skip loading and caching all domains!")
            return
        }

        // Load all domains with active=true
        val criteria = Criteria(SysDomain::active eq true)
        val domains = dao.searchAs<SysDomainCacheEntry>(criteria)
        log.debug("Loaded ${domains.size} domains from database.")

        // Clear cache
        if (clear) {
            clear()
        }

        // Cache domains
        domains.forEach {
            val domain = it.domain
            KeyValueCacheKit.put(CACHE_NAME, domain, it)
        }
        log.debug("Cached ${domains.size} domains.")
    }

    /**
     * Get domain from cache by name; if absent, load from database and write to cache.
     *
     * @param domain domain name
     * @return SysDomainCacheEntry instance, or null if not found
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#domain",
        unless = "#result == null"
    )
    open fun getDomain(domain: String): SysDomainCacheEntry? {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Domain ${domain} not in cache, loading from database...")
        }

        val criteria = Criteria.and(
            SysDomain::domain eq domain,
            SysDomain::active eq true
        )
        val domains = dao.searchAs<SysDomainCacheEntry>(criteria)
        return if (domains.isEmpty()) {
            log.debug("No active=true domain named ${domain} found in database.")
            null
        } else {
            log.debug("Loaded domain ${domain} from database.")
            domains.first()
        }
    }

    /**
     * Sync cache after a database insert.
     *
     * @param any object containing the required properties
     * @param id domain id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME) && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("After inserting domain with id ${id}, syncing ${CACHE_NAME} cache...")
            val domain = BeanKit.getProperty(any, SysDomain::domain.name) as String
            KeyValueCacheKit.evict(CACHE_NAME, domain) // evict cache
            getSelf<DomainByNameCache>().getDomain(domain) // re-cache
            log.debug("${CACHE_NAME} cache sync completed.")
        }
    }

    /**
     * Sync cache after a database update.
     *
     * @param any object containing the required properties
     * @param id domain id
     */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After updating domain with id ${id}, syncing ${CACHE_NAME} cache...")
            val domain = if (any == null) {
                dao.get(id)?.domain
            } else {
                BeanKit.getProperty(any, SysDomain::domain.name) as String
            }
            if (domain.isNullOrBlank()) return
            KeyValueCacheKit.evict(CACHE_NAME, domain) // evict cache
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<DomainByNameCache>().getDomain(domain) // re-cache
            }
            log.debug("${CACHE_NAME} cache sync completed.")
        }
    }

    /**
     * Sync cache after a database delete.
     *
     * @param any object containing the required properties
     * @param id domain id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After deleting domain with id ${id}, evicting from ${CACHE_NAME} cache...")
            val domain = BeanKit.getProperty(any, SysDomain::domain.name) as String
            KeyValueCacheKit.evict(CACHE_NAME, domain) // evict cache
            log.debug("${CACHE_NAME} cache sync completed.")
        }
    }

    /**
     * Sync cache after a batch database delete.
     *
     * @param ids domain id collection
     * @param domains domain name collection
     */
    open fun syncOnBatchDelete(ids: Collection<String>, domains: Set<String>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After batch deleting domains with ids ${ids}, evicting from ${CACHE_NAME} cache...")
            domains.forEach {
                KeyValueCacheKit.evict(CACHE_NAME, it) // evict cache
            }
            log.debug("${CACHE_NAME} cache sync completed.")
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDomainInserted) {
        // After AFTER_COMMIT the DB row is visible; look up the domain name by id for the cache key
        val domain = dao.get(event.id) ?: return
        syncOnInsert(domain, event.id)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDomainUpdated): Unit = syncOnUpdate(null, event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDomainDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, event.domain)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysDomainBatchDeleted): Unit = syncOnBatchDelete(event.ids, event.domains)

    private val log = LogFactory.getLog(this::class)

}
