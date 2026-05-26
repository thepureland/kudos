package io.kudos.ms.sys.core.cache.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.data.json.JsonKit
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
import io.kudos.ms.sys.common.cache.enums.SysCacheErrorCodeEnum
import io.kudos.ms.sys.common.cache.vo.SysCacheCacheEntry
import io.kudos.ms.sys.core.cache.cache.SysCacheHashCache
import io.kudos.ms.sys.core.cache.dao.SysCacheDao
import io.kudos.ms.sys.core.cache.event.SysCacheBatchDeleted
import io.kudos.ms.sys.core.cache.event.SysCacheDeleted
import io.kudos.ms.sys.core.cache.event.SysCacheInserted
import io.kudos.ms.sys.core.cache.event.SysCacheUpdated
import io.kudos.ms.sys.core.cache.model.po.SysCache
import io.kudos.ms.sys.core.cache.service.iservice.ISysCacheService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * Cache configuration service.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysCacheService(
    dao: SysCacheDao,
    private val sysCacheHashCache: SysCacheHashCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysCache, SysCacheDao>(dao), ISysCacheService {

    private val log = LogFactory.getLog(this::class)

    @Suppress("UNCHECKED_CAST")
    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == SysCacheCacheEntry::class) sysCacheHashCache.getCacheById(id) as R?
        else super.get(id, returnType)

    @Transactional(readOnly = true)
    override fun getCacheFromCache(id: String): SysCacheCacheEntry? = sysCacheHashCache.getCacheById(id)

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "Inserted cache configuration id=$id.") {
            eventPublisher.publishEvent(SysCacheInserted(id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "cache configuration")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated cache configuration id=$id.",
            failureMessage = "Failed to update cache configuration id=$id!",
        ) {
            eventPublisher.publishEvent(SysCacheUpdated(id))
        }
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val cache = SysCache {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(cache),
            log = log,
            successMessage = "Updated cache configuration id=$id active=$active.",
            failureMessage = "Failed to update cache configuration id=$id active=$active!",
        ) {
            eventPublisher.publishEvent(SysCacheUpdated(id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val sysCache = dao.get(id)
        if (sysCache == null) {
            log.warn("Cache configuration id=$id no longer exists when attempting delete!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted cache configuration id=$id.",
            failureMessage = "Failed to delete cache configuration id=$id!",
        ) {
            eventPublisher.publishEvent(SysCacheDeleted(id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("Batch delete cache configurations: expected ${ids.size}, actually deleted $count.")
        if (count > 0 && ids.isNotEmpty()) {
            eventPublisher.publishEvent(SysCacheBatchDeleted(ids))
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun getCachesFromCache(atomicServiceCode: String): List<SysCacheCacheEntry> = sysCacheHashCache.getCaches(atomicServiceCode)

    @Transactional(readOnly = true)
    override fun reload(id: String, key: String) {
        require(id.isNotBlank())
        require(key.isNotBlank())
        withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                requireKeyExists(name, key, keyValueCache = true)
                KeyValueCacheKit.reload(name, key)
            } else {
                requireKeyExists(name, key, keyValueCache = false)
                HashCacheKit.reload(name, key)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun reloadAll(id: String) {
        require(id.isNotBlank())
        withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                KeyValueCacheKit.reloadAll(name)
            } else {
                HashCacheKit.reloadAll(name)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun evict(id: String, key: String) {
        require(id.isNotBlank())
        require(key.isNotBlank())
        withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                requireKeyExists(name, key, keyValueCache = true)
                KeyValueCacheKit.evict(name, key)
            } else {
                requireKeyExists(name, key, keyValueCache = false)
                HashCacheKit.evict(name, key)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun evictAll(id: String) {
        require(id.isNotBlank())
        withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                KeyValueCacheKit.clear(name)
            } else {
                HashCacheKit.clear(name)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun existsKey(id: String, key: String): Boolean {
        require(id.isNotBlank())
        require(key.isNotBlank())
        return withCacheConfig(id) { cache ->
            existsKey(cache.name, key, isKeyValueCache(cache))
        }
    }

    @Transactional(readOnly = true)
    override fun getValueJson(id: String, key: String): String {
        require(id.isNotBlank())
        require(key.isNotBlank())
        val value = withCacheConfig(id) { cache ->
            val name = cache.name
            if (isKeyValueCache(cache)) {
                requireKeyExists(name, key, keyValueCache = true)
                KeyValueCacheKit.getValue(name, key)
            } else {
                requireKeyExists(name, key, keyValueCache = false)
                HashCacheKit.getValue(name, key)
            }
        }
        return JsonKit.toJson(value)
    }

    /** Fetch cache configuration by id; throws if not found. */
    private fun getCacheConfigById(id: String): SysCacheCacheEntry =
        sysCacheHashCache.getCacheById(id) ?: throw ServiceException(SysCacheErrorCodeEnum.CACHE_CONFIG_NOT_FOUND)

    /**
     * Fetch the cache configuration by id and run [block]: shared by 6 call sites (reload / evict / getValue / ...) as the
     * "load config → dispatch keyValue/hash" template; was once accidentally removed by a docs commit which broke main.
     *
     * @param T block return type
     * @param id cache configuration id
     * @param block execution block; the parameter is the resolved [SysCacheCacheEntry]
     * @return the value returned by [block]
     * @throws ServiceException when the cache configuration does not exist
     */
    private inline fun <T> withCacheConfig(id: String, block: (SysCacheCacheEntry) -> T): T =
        block(getCacheConfigById(id))

    private fun requireKeyExists(name: String, key: String, keyValueCache: Boolean) {
        if (!existsKey(name, key, keyValueCache)) throw ServiceException(SysCacheErrorCodeEnum.CACHE_KEY_NOT_FOUND)
    }

    /**
     * Dispatch existsKey by cache shape: keyValue goes through [KeyValueCacheKit.existsKey], hash through [HashCacheKit.existsById].
     *
     * @param name cache region name
     * @param key cache key (or hash field name)
     * @param keyValueCache true for KV cache, false for Hash cache
     * @return whether the key exists
     * @author K
     * @since 1.0.0
     */
    private fun existsKey(name: String, key: String, keyValueCache: Boolean): Boolean =
        if (keyValueCache) KeyValueCacheKit.existsKey(name, key) else HashCacheKit.existsById(name, key)

    /**
     * Determine cache shape: `hash=false` means a KeyValue cache.
     *
     * @param cache cache configuration entry
     * @return true if it is a KV cache
     * @author K
     * @since 1.0.0
     */
    private fun isKeyValueCache(cache: SysCacheCacheEntry): Boolean = !cache.hash
}
