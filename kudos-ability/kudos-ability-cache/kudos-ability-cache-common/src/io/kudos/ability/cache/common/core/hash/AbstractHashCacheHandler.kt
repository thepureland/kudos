package io.kudos.ability.cache.common.core.hash

import io.kudos.ability.cache.common.core.AbstractCacheHandler
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.reflect.KClass

/**
 * Abstract base class for hash-type cache handlers.
 *
 * @param T cache entry type (must be a subtype of [IIdEntity])
 * @author K
 * @since 1.0.0
 */
abstract class AbstractHashCacheHandler<T : IIdEntity<*>> : AbstractCacheHandler<T>() {

    private val log = LogFactory.getLog(this::class)

    /** Cache entity type, used to specify the type when deleting/writing back by id. */
    protected abstract fun entityClass(): KClass<T>

    /**
     * Exposes the entity type for callers such as [HashCacheKit] that look up by cacheName, for use in untyped getValue.
     * Declared `open` so that Spring CGLIB subclass proxies can override it, avoiding the
     * `Public final method cannot get proxied via CGLIB` warning.
     */
    open fun exposedEntityClass(): KClass<*> = entityClass()

    /** Secondary property names used for Set indexes; must match those used on write/delete. Override as needed. */
    protected open fun filterableProperties(): Set<String> = emptySet()

    /** Secondary property names used for ZSet indexes; must match those used on write/delete. Override as needed. */
    protected open fun sortableProperties(): Set<String> = emptySet()

    /**
     * Reloads a single cache entry by id: first removes the id from the hash, then writes it back if [doReload]
     * returns a non-null value. Subclasses can override [doReload] to load from a database/source; the default
     * only deletes without writing back.
     *
     * @param id entity primary key
     */
    @Suppress("UNCHECKED_CAST")
    open fun reload(id: Any) {
        if (!HashCacheKit.isCacheActive(cacheName())) return
        val cache = hashCache()
        cache.deleteById(
            cacheName(),
            id,
            entityClass() as KClass<IIdEntity<Any?>>,
            filterableProperties(),
            sortableProperties()
        )
        log.info("Manually reloading hash cache [${cacheName()}], id=$id ...")
        val entity = doReload(id)
        if (entity == null) {
            log.info("The corresponding entry no longer exists in the data source.")
        } else {
            cache.saveBatch(
                cacheName(),
                listOf(entity) as List<IIdEntity<Any?>>,
                filterableProperties(),
                sortableProperties()
            )
            log.info("Reload succeeded.")
        }
    }

    /**
     * Loads an entity from the data source by id for [reload] to write back. Returns null by default (delete only).
     *
     * @param id entity primary key
     * @return the loaded entity, or null if not present
     */
    protected open fun doReload(id: Any): T? = null

    /**
     * Evicts the hash cache entry for the given id (delete only, no write-back).
     *
     * @param id entity primary key
     */
    @Suppress("UNCHECKED_CAST")
    open fun evict(id: Any) {
        if (!HashCacheKit.isCacheActive(cacheName())) return
        hashCache().deleteById(
            cacheName(),
            id,
            entityClass() as KClass<IIdEntity<Any?>>,
            filterableProperties(),
            sortableProperties()
        )
    }

    /**
     * Returns the hash cache region associated with this handler (as reported by [cacheName] in the subclass).
     * The subclass's batch/single accessors all work against this cache instance; it is exposed separately so
     * subclasses can obtain the underlying cache object directly when needed.
     *
     * @return the hash cache bound to the current cacheName
     * @author K
     * @since 1.0.0
     */
    protected fun hashCache() = HashCacheKit.getHashCache(cacheName())
}