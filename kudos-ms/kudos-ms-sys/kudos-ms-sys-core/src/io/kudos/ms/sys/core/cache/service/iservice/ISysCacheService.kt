package io.kudos.ms.sys.core.cache.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.cache.vo.SysCacheCacheEntry
import io.kudos.ms.sys.core.cache.model.po.SysCache


/**
 * Cache service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysCacheService : IBaseCrudService<String, SysCache> {

    /**
     * Load a cache configuration by primary key id and cache the result.
     *
     * @param id cache configuration primary key, non-blank
     * @return cache detail object, or null when not found
     */
    fun getCacheFromCache(id: String): SysCacheCacheEntry?

    /**
     * Update the active flag and synchronize the cache.
     *
     * @param id primary key
     * @param active whether to activate
     * @return whether the update succeeded
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * Get the cache configuration list for an atomic service from the cache.
     *
     * @param atomicServiceCode atomic service code
     * @return list of cache records
     */
    fun getCachesFromCache(atomicServiceCode: String): List<SysCacheCacheEntry>

    /**
     * Reload a single cache entry under the given cache configuration (by id) and key.
     *
     * @param id cache configuration primary key, non-blank
     * @param key cache key
     */
    fun reload(id: String, key: String)

    /**
     * Reload all cache entries under the given cache configuration (by id).
     *
     * @param id cache configuration primary key, non-blank
     */
    fun reloadAll(id: String)

    /**
     * Evict a single cache entry under the given cache configuration (by id) and key.
     *
     * @param id cache configuration primary key, non-blank
     * @param key cache key
     */
    fun evict(id: String, key: String)

    /**
     * Evict all cache entries under the given cache configuration (by id).
     *
     * @param id cache configuration primary key, non-blank
     */
    fun evictAll(id: String)

    /**
     * Check whether a given key exists under the given cache configuration (by id).
     *
     * @param id cache configuration primary key, non-blank
     * @param key cache key
     */
    fun existsKey(id: String, key: String): Boolean

    /**
     * Get the JSON representation of the value of the given key under the given cache configuration (by id).
     *
     * @param id cache configuration primary key, non-blank
     * @param key cache key
     * @return JSON string of the value; empty string when value is null or on error
     */
    fun getValueJson(id: String, key: String): String


}
