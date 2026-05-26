package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.lang.GenericKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.support.dao.IBaseReadOnlyDao
import io.kudos.base.model.payload.MutableListSearchPayload
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass

/**
 * Abstract cache handler whose key is an id.
 *
 * @param T value type
 * @author K
 * @since 1.0.0
 */
abstract class AbstractByIdCacheHandler<PK : Any, T : IIdEntity<*>, DAO : IBaseReadOnlyDao<PK, *>> :
    AbstractKeyValueCacheHandler<T>() {

    @Autowired
    protected lateinit var dao: DAO

    /**
     * Loads the corresponding record from the database by primary key.
     *
     * @param id primary key
     * @return cached object
     */
    protected fun getById(id: PK): T? {
        if (id is CharSequence) {
            require(id.isNotEmpty()) { log.error("When fetching ${itemDesc()} from cache ${cacheName()}, id must not be empty!") }
        }
        if (KeyValueCacheKit.isCacheActive(cacheName())) {
            log.debug("${itemDesc()} with id ${id} not found in cache; loading from database...")
        }
        val result = dao.get(id, getCacheItemClass())
        if (result == null) {
            log.warn("${itemDesc()} with id ${id} does not exist in the database!")
        } else {
            log.debug("Loaded ${itemDesc()} with id ${id} from database.")
        }
        return result
    }

    /**
     * Batch-loads records from the database by a collection of primary keys.
     *
     * @param ids primary key collection
     * @return Map<primary key, cached object>
     */
    protected fun getByIds(ids: Collection<PK>): Map<String, T> {
        require(ids.isNotEmpty()) { log.error("When batch-fetching ${itemDesc()} from cache ${cacheName()}, id collection must not be empty!") }
        if (KeyValueCacheKit.isCacheActive(cacheName())) {
            log.debug("Not all ${itemDesc()} with ids ${ids} were found in cache ${cacheName()}; loading from database...")
        }
        val searchPayload = MutableListSearchPayload().apply {
            setReturnEntityClass(getCacheItemClass())
            setCriterions(listOf(Criterion("id", OperatorEnum.IN, ids)))
        }

        val results = dao.search(searchPayload, getCacheItemClass())
        log.debug("Loaded ${results.size} ${itemDesc()} record(s) from database.")
        return results
            .mapNotNull { item ->
                val usableId = toUsableId(item.id) ?: return@mapNotNull null
                usableId.toString() to item
            }
            .toMap()
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(cacheName())) {
            log.info("Cache is disabled; will not load and cache all ${itemDesc()} entries!")
            return
        }

        // load all
        val searchPayload = MutableListSearchPayload().apply {
            setReturnEntityClass(getCacheItemClass())
        }

        val results = dao.search(searchPayload, getCacheItemClass())
        log.debug("Loaded ${results.size} ${itemDesc()} entries from database.")

        // clear the cache first
        if (clear) {
            clear()
        }

        // put into cache
        results.forEach {
            val usableId = toUsableId(it.id)
            if (usableId != null) {
                KeyValueCacheKit.put(cacheName(), usableId, it)
            } else {
                log.warn("Skipping caching of ${itemDesc()} record with blank id: $it")
            }
        }

        log.debug("Cached ${results.size} ${itemDesc()} entries.")
    }

    /**
     * Synchronizes the cache after inserting a new database record.
     *
     * @param id primary key
     */
    open fun syncOnInsert(id: PK) {
        if (KeyValueCacheKit.isCacheActive(cacheName()) && KeyValueCacheKit.isWriteInTime(cacheName())) {
            log.debug("After inserting ${itemDesc()} with id ${id}, syncing cache ${cacheName()}...")
            doReload(id.toString()) // cache
            log.debug("Cache ${cacheName()} sync completed.")
        }
    }

    /**
     * Synchronizes the cache after updating a database record.
     *
     * @param id primary key
     */
    open fun syncOnUpdate(id: PK) {
        if (KeyValueCacheKit.isCacheActive(cacheName())) {
            log.debug("After updating ${itemDesc()} with id ${id}, syncing cache ${cacheName()}...")
            KeyValueCacheKit.evict(cacheName(), id) // evict from cache
            if (KeyValueCacheKit.isWriteInTime(cacheName())) {
                doReload(id.toString()) // cache
            }
            log.debug("Cache ${cacheName()} sync completed.")
        }
    }

    /**
     * Synchronizes the cache after deleting a database record.
     *
     * @param id primary key
     */
    open fun syncOnDelete(id: PK) {
        if (KeyValueCacheKit.isCacheActive(cacheName())) {
            log.debug("After deleting ${itemDesc()} with id ${id}, syncing cache ${cacheName()}...")
            KeyValueCacheKit.evict(cacheName(), id) // evict from cache
            log.debug("Cache ${cacheName()} sync completed.")
        }
    }

    /**
     * Synchronizes the cache after batch-deleting database objects.
     *
     * @param ids primary key collection
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (KeyValueCacheKit.isCacheActive(cacheName())) {
            log.debug("After batch-deleting ${itemDesc()} with ids ${ids}, syncing eviction from cache ${cacheName()}...")
            ids.forEach {
                KeyValueCacheKit.evict(cacheName(), it) // evict cache entry
            }
            log.debug("Cache ${cacheName()} sync completed.")
        }
    }

    /**
     * Returns the cache item type.
     *
     * @return cache item type
     */
    protected fun getCacheItemClass(): KClass<T> {
        @Suppress("UNCHECKED_CAST")
        return GenericKit.getSuperClassGenricClass(this::class, 1) as KClass<T>
    }

    /**
     * Returns the cache item description.
     *
     * @return cache item description
     */
    protected abstract fun itemDesc(): String

    private fun toUsableId(id: Any?): Any? = when (id) {
        null -> null
        is CharSequence -> if (id.isNotBlank()) id else null
        else -> id
    }

    private val log = LogFactory.getLog(this::class)

}