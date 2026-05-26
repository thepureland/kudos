package io.kudos.ms.sys.core.locale.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.dao.SysLocaleDao
import io.kudos.ms.sys.core.locale.event.SysLocaleBatchDeleted
import io.kudos.ms.sys.core.locale.event.SysLocaleDeleted
import io.kudos.ms.sys.core.locale.event.SysLocaleInserted
import io.kudos.ms.sys.core.locale.event.SysLocaleUpdated
import io.kudos.ms.sys.core.locale.model.po.SysLocale
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Cache handler for the language/locale dictionary.
 *
 * 1. Source table: sys_locale
 * 2. Caches only languages with active=true
 * 3. Cache key: locale code (e.g. zh_CN)
 * 4. Cache value: [SysLocaleCacheEntry]
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class LocaleByCodeCache : AbstractKeyValueCacheHandler<SysLocaleCacheEntry>() {

    @Autowired
    private lateinit var dao: SysLocaleDao

    companion object {
        private const val CACHE_NAME = "SYS_LOCALE_BY_CODE"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): SysLocaleCacheEntry? {
        return getSelf<LocaleByCodeCache>().getLocale(key)
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache is disabled; skipping load and cache of all locales!")
            return
        }
        val criteria = Criteria(SysLocale::active eq true)
        val locales = dao.searchAs<SysLocaleCacheEntry>(criteria)
        log.debug("Loaded ${locales.size} locales from the database.")
        if (clear) clear()
        locales.forEach { KeyValueCacheKit.put(CACHE_NAME, it.code, it) }
        log.debug("Cached ${locales.size} locales.")
    }

    /**
     * Fetch a locale by code; on cache miss, load from DB and write back.
     *
     * @param code locale code
     * @return cache entry; null if not found or not active
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#code",
        unless = "#result == null"
    )
    open fun getLocale(code: String): SysLocaleCacheEntry? {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Locale with code=$code not in cache; loading from database...")
        }
        val criteria = Criteria.and(
            SysLocale::code eq code,
            SysLocale::active eq true,
        )
        val locales = dao.searchAs<SysLocaleCacheEntry>(criteria)
        return if (locales.isEmpty()) {
            log.debug("No active=true locale with code=$code found in the database.")
            null
        } else {
            log.debug("Loaded locale with code=$code from the database.")
            locales.first()
        }
    }

    /** Sync cache after a record is inserted in the database. */
    open fun syncOnInsert(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME) && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            val code = BeanKit.getProperty(any, SysLocale::code.name) as String
            log.debug("Syncing cache $CACHE_NAME after inserting locale with id=$id...")
            KeyValueCacheKit.evict(CACHE_NAME, code)
            getSelf<LocaleByCodeCache>().getLocale(code)
        }
    }

    /** Sync cache after a record is updated in the database. */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            val code = if (any == null) dao.get(id)?.code else BeanKit.getProperty(any, SysLocale::code.name) as String
            if (code.isNullOrBlank()) return
            log.debug("Syncing cache $CACHE_NAME after updating locale with id=$id...")
            KeyValueCacheKit.evict(CACHE_NAME, code)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<LocaleByCodeCache>().getLocale(code)
            }
        }
    }

    /** Sync cache after a record is deleted in the database. */
    open fun syncOnDelete(code: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Evicting locale code=$code from cache $CACHE_NAME after delete...")
            KeyValueCacheKit.evict(CACHE_NAME, code)
        }
    }

    /** Sync cache after a batch delete. */
    open fun syncOnBatchDelete(codes: Set<String>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Evicting ${codes.size} locales from cache $CACHE_NAME after batch delete...")
            codes.forEach { KeyValueCacheKit.evict(CACHE_NAME, it) }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysLocaleInserted) {
        val po = dao.get(event.id) ?: return
        syncOnInsert(po, event.id)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysLocaleUpdated): Unit = syncOnUpdate(null, event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysLocaleDeleted): Unit = syncOnDelete(event.code)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysLocaleBatchDeleted): Unit = syncOnBatchDelete(event.codes)

    private val log = LogFactory.getLog(this::class)

}
