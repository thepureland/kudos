package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.core.keyvalue.MixCache
import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheOperatorVo
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.cache.Cache
import org.springframework.stereotype.Component
import kotlin.reflect.KClass


/**
 * Cache utility.
 *
 * @author K
 * @since 1.0.0
 */
@Component
object KeyValueCacheKit {

    private val log = LogFactory.getLog(this::class)

    /**
     * Whether caching is enabled. Both the global switch and the cache-specific switch must be on.
     *
     * @param cacheName cache name
     * @return true: enabled; false: disabled
     * @author K
     * @since 1.0.0
     */
    fun isCacheActive(cacheName: String): Boolean =
        getCacheConfigProvider().getCacheConfig(cacheName)?.isActive ?: false

    /**
     * Returns the cache by name.
     *
     * @param name cache name
     * @return the cache object
     * @author K
     * @since 1.0.0
     */
    fun getCache(name: String): Cache? {
        val cacheManager = getCacheManager() ?: return null
        return cacheManager.getCache(name).also {
            if (it == null) log.error("Cache [$name] does not exist!")
        }
    }

    /**
     * Returns the value of the given key in the specified cache.
     *
     * @param cacheName  cache name
     * @param key        cache key
     * @param valueClass type of the value for the cache key
     * @return the value associated with the cache key
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getValue(cacheName: String, key: Any, valueClass: KClass<T>): T? {
        val cache = getCache(cacheName) ?: return null
        return cache.get<T>(key, valueClass.java)
    }

    /**
     * Returns the value of the given key in the specified cache.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @return the value associated with the cache key
     * @author K
     * @since 1.0.0
     */
    fun getValue(cacheName: String, key: Any): Any? = getCache(cacheName)?.get(key)?.get()

    /**
     * Writes a value to the cache.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @param value     value to cache
     * @author K
     * @since 1.0.0
     */
    fun put(cacheName: String, key: Any, value: Any?) {
        if (!isCacheActive(cacheName)) return
        getCache(cacheName)?.put(key, value)
    }

    /**
     * Writes a value to the cache if absent.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @param value     value to cache
     * @author K
     * @since 1.0.0
     */
    fun putIfAbsent(cacheName: String, key: Any, value: Any?) {
        if (!isCacheActive(cacheName)) return
        getCache(cacheName)?.putIfAbsent(key, value)
    }

    /**
     * Evicts a cache entry and triggers a dependency notification.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @author K
     * @since 1.0.0
     */
    fun evict(cacheName: String, key: Any) {
        if (!isCacheActive(cacheName)) return
        val cache = getCache(cacheName) as? MixCache ?: return
        // Single-node local cache (SINGLE_LOCAL) is unreachable across processes, so broadcast a notification
        // and let each node evict locally; for other strategies the remote layer is authoritative, evict directly.
        if (cache.strategy == CacheStrategy.SINGLE_LOCAL) {
            CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, cacheName, key).doNotify()
        } else {
            doEvict(cacheName, key)
        }
    }

    /**
     * Evicts a cache entry.
     *
     * @param cacheName cache name
     * @param key       cache key
     * @author K
     * @since 1.0.0
     */
    fun doEvict(cacheName: String, key: Any) {
        if (!isCacheActive(cacheName)) return
        getCache(cacheName)?.evict(key)
    }

    /**
     * Clears the cache, broadcasting a notification.
     *
     * @param cacheName cache name
     * @author K
     * @since 1.0.0
     */
    fun clear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        val cache = getCache(cacheName) as? MixCache ?: return
        if (cache.strategy == CacheStrategy.SINGLE_LOCAL) {
            CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, cacheName, null).doNotify()
        } else {
            doClear(cacheName)
        }
    }

    /**
     * Clears the cache.
     *
     * @param cacheName cache name
     * @author K
     * @since 1.0.0
     */
    fun doClear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        getCache(cacheName)?.clear()
    }

    /**
     * Whether the cache is written back immediately after an insert or update.
     *
     * @param cacheName cache name
     * @return true: write back immediately; otherwise false. Returns false when the cache does not exist.
     * @author K
     * @since 1.0.0
     */
    fun isWriteInTime(cacheName: String): Boolean {
        if (!isCacheActive(cacheName)) return false
        return getCacheConfig(cacheName)?.isWriteInTime ?: false
    }

    /**
     * Returns the cache configuration by name.
     *
     * @param cacheName cache name
     * @return the cache configuration; null if not found
     * @author K
     * @since 1.0.0
     */
    fun getCacheConfig(cacheName: String): CacheConfig? {
        if (!isCacheActive(cacheName)) return null
        return getCacheConfigProvider().getCacheConfig(cacheName).also {
            if (it == null) log.warn("Cache [$cacheName] does not exist!")
        }
    }

    /**
     * Reloads a cache entry.
     *
     * @param cacheName cache name
     * @param key       key
     */
    fun reload(cacheName: String, key: String) {
        if (!isCacheActive(cacheName)) return
        val cacheConfig = getCacheConfig(cacheName) ?: return
        if (cacheConfig.isWriteOnBoot) {
            handlersFor(cacheName).forEach { it.reload(key) }
        } else {
            evict(cacheName, key)
        }
    }

    /**
     * Reloads all entries in the cache.
     *
     * @param cacheName cache name
     */
    fun reloadAll(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        val cacheConfig = getCacheConfig(cacheName) ?: return
        if (cacheConfig.isWriteOnBoot) {
            handlersFor(cacheName).forEach { it.reloadAll(true) }
        } else {
            clear(cacheName)
        }
    }

    /**
     * Centralizes the "look up Handler by cacheName" boilerplate (same idea as [HashCacheKit.handlersFor]).
     *
     * The index is lazily built with double-checked locking: the first call scans once and groupBy is stored
     * in [handlerIndex], and subsequent calls do a direct map lookup. Newly added handler beans will not be
     * picked up automatically; the index is rebuilt by [resetForTesting] or by restarting the context. This
     * avoids scanning Spring beans on every reload / reloadAll.
     */
    @Volatile private var handlerIndex: Map<String, List<AbstractKeyValueCacheHandler<*>>>? = null

    private fun handlersFor(cacheName: String): List<AbstractKeyValueCacheHandler<*>> {
        val index = handlerIndex ?: synchronized(this) {
            handlerIndex ?: SpringKit.getBeansOfType<AbstractKeyValueCacheHandler<*>>().values
                .groupBy { it.cacheName() }
                .also { handlerIndex = it }
        }
        return index[cacheName].orEmpty()
    }

    /**
     * Evicts cache entries whose keys start with the given prefix.
     * @param cacheName cache name
     * @param keyPattern key prefix
     */
    fun evictByPattern(cacheName: String, keyPattern: String) {
        if (!isCacheActive(cacheName)) return
        val cacheManager = getCacheManager() ?: return
        cacheManager.evictByPattern(cacheName, keyPattern)
    }

    /**
     * Whether the specified key exists in the cache (independent of whether the value is null);
     * under LOCAL_REMOTE, presence at either layer counts as present.
     *
     * @param cacheName cache name
     * @param key cache key
     * @return true: present; false: absent
     */
    fun existsKey(cacheName: String, key: String): Boolean {
        if (!isCacheActive(cacheName)) return false
        return getCacheManager()?.existsKey(cacheName, key) ?: false
    }

    /**
     * Returns the cache manager.
     *
     * When caching is disabled (kudos.ability.cache.enabled=false), the mixCacheManager bean is not created,
     * and this method returns null; callers must handle null.
     *
     * @return MixCacheManager, or null if caching is disabled
     */
    private fun getCacheManager(): MixCacheManager? =
        cacheManagerOverride ?: (SpringKit.getBeanOrNull("mixCacheManager") as? MixCacheManager)

    /**
     * Returns the cache config provider.
     *
     * @return ICacheConfigProvider
     */
    private fun getCacheConfigProvider(): ICacheConfigProvider =
        configProviderOverride ?: SpringKit.getBean<ICacheConfigProvider>()

    // ---- Test injection hooks --------------------------------------------------
    // Kit is an `object` singleton; the production path still resolves via SpringKit.getBean and behavior is unchanged.
    // Unit tests that do not want to bring up a Spring context can inject mocks via the overrides below and
    // call resetForTesting afterwards.

    @Volatile private var cacheManagerOverride: MixCacheManager? = null
    @Volatile private var configProviderOverride: ICacheConfigProvider? = null

    /**
     * Test-only: injects dependencies temporarily so unit tests do not need to start a full Spring context.
     * Passing null for any parameter falls back to the default [SpringKit] lookup path.
     * [resetForTesting] must be called at the end of the test to restore state; otherwise it will contaminate
     * subsequent tests in the same JVM.
     */
    fun overrideForTesting(
        cacheManager: MixCacheManager? = null,
        configProvider: ICacheConfigProvider? = null,
    ) {
        cacheManagerOverride = cacheManager
        configProviderOverride = configProvider
    }

    /**
     * Test-only: clears the mocks injected by [overrideForTesting] and restores Spring lookup.
     */
    fun resetForTesting() {
        cacheManagerOverride = null
        configProviderOverride = null
        handlerIndex = null
    }

}
