package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.HashCacheKit.getHashCache
import io.kudos.ability.cache.common.notify.CacheOperatorVo
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.kit.SpringKit
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Hash cache toolkit: looks up [IHashCache] (the strategy-wrapped unified abstraction) by cacheName.
 *
 * Similar to [KeyValueCacheKit]; the caller can freely choose one of three strategies via configuration
 * (SINGLE_LOCAL / REMOTE / LOCAL_REMOTE). Throws [IllegalStateException] if [cacheName] is not registered
 * in the configuration.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
object HashCacheKit {

    private val log = LogFactory.getLog(this::class)

    /**
     * Whether hash caching is enabled. Both the global switch and the cache-specific switch must be on.
     *
     * @param cacheName cache name
     * @return true: enabled; false: disabled
     */
    fun isCacheActive(cacheName: String): Boolean =
        getConfigProvider()?.getHashCacheConfigs()?.get(cacheName)?.isActive == true

    /**
     * Returns the hash cache configuration by name.
     *
     * @param cacheName cache name
     * @return the cache configuration; null if not found
     */
    fun getCacheConfig(cacheName: String): CacheConfig? {
        val configProvider = getConfigProvider() ?: return null
        val config = configProvider.getHashCacheConfigs()[cacheName]
        if (config == null) {
            log.warn("Hash cache [$cacheName] does not exist!")
        }
        return config
    }

    /**
     * Whether the hash cache is written back immediately after an insert or update.
     *
     * @param cacheName cache name
     * @return true: write back immediately; otherwise false. Returns false when the cache does not exist.
     */
    fun isWriteInTime(cacheName: String): Boolean {
        if (!isCacheActive(cacheName)) return false
        return getCacheConfig(cacheName)?.isWriteInTime == true
    }

    /**
     * Evicts the hash cache entry for the given id (broadcasts a notification under SINGLE_LOCAL,
     * otherwise deletes directly).
     *
     * @param cacheName cache name
     * @param id        entity primary key
     */
    fun evict(cacheName: String, id: Any) {
        if (!isCacheActive(cacheName)) return
        val config = getCacheConfig(cacheName) ?: return
        if (config.resolvedStrategy == CacheStrategy.SINGLE_LOCAL) {
            CacheOperatorVo(CacheOperatorVo.TYPE_EVICT, cacheName, id).doNotify()
        } else {
            doEvict(cacheName, id)
        }
    }

    /**
     * Evicts the hash cache entry for the given id (direct delete, no notification).
     *
     * @param cacheName cache name
     * @param id        entity primary key
     */
    fun doEvict(cacheName: String, id: Any) {
        if (!isCacheActive(cacheName)) return
        handlersFor(cacheName).forEach { it.evict(id) }
    }

    /**
     * Clears the hash cache (broadcasts a notification under SINGLE_LOCAL, otherwise clears directly).
     *
     * @param cacheName cache name
     */
    fun clear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        val config = getCacheConfig(cacheName) ?: return
        if (config.resolvedStrategy == CacheStrategy.SINGLE_LOCAL) {
            CacheOperatorVo(CacheOperatorVo.TYPE_CLEAR, cacheName, null).doNotify()
        } else {
            doClear(cacheName)
        }
    }

    /**
     * Clears the hash cache (direct clear, no notification).
     *
     * @param cacheName cache name
     */
    fun doClear(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        getHashCache(cacheName).clear(cacheName)
    }

    /**
     * Reloads the hash cache entry for the given id.
     * Delegates to the Handler associated with this cacheName: first removes the id from the hash, then —
     * if writeOnBoot is configured and the Handler implements [AbstractHashCacheHandler.doReload] — loads
     * from the data source and writes it back.
     *
     * @param cacheName cache name
     * @param id        entity primary key
     */
    fun reload(cacheName: String, id: Any) {
        if (!isCacheActive(cacheName)) return
        handlersFor(cacheName).forEach { it.reload(id) }
    }

    /**
     * Reloads all entries in the hash cache.
     *
     * @param cacheName cache name
     */
    fun reloadAll(cacheName: String) {
        if (!isCacheActive(cacheName)) return
        val cacheConfig = getCacheConfig(cacheName) ?: return
        if (cacheConfig.isWriteOnBoot) {
            handlersFor(cacheName).forEach { it.reloadAll(true) }
        } else {
            getHashCache(cacheName).clear(cacheName)
        }
    }

    /**
     * Lightweight check for whether the given id exists in the hash cache (does not deserialize the value;
     * uses containsKey/HEXISTS).
     *
     * @param cacheName cache name
     * @param id        entity id
     * @return true: present; false: absent
     */
    fun existsById(cacheName: String, id: Any): Boolean {
        if (!isCacheActive(cacheName)) return false
        return getHashCache(cacheName).existsById(cacheName, id)
    }

    /**
     * Returns the entity value for the given id from the hash cache (typed).
     *
     * @param cacheName  cache name
     * @param id         entity primary key
     * @param valueClass entity type
     * @return the entity, or null if absent
     */
    fun <PK, T : IIdEntity<PK>> getValue(cacheName: String, id: PK, valueClass: KClass<T>): T? {
        if (!isCacheActive(cacheName)) return null
        return getHashCache(cacheName).getById(cacheName, id, valueClass)
    }

    /**
     * Returns the entity value for the given id from the hash cache (untyped, returns Any?).
     * Internally deserializes via the entity type exposed by the Handler associated with this cacheName.
     *
     * @param cacheName cache name
     * @param id        entity primary key
     * @return the entity, or null if absent or no Handler is configured
     */
    fun getValue(cacheName: String, id: Any): Any? {
        if (!isCacheActive(cacheName)) return null
        val handler = handlersFor(cacheName).firstOrNull() ?: return null
        @Suppress("UNCHECKED_CAST")
        val entityClass = handler.exposedEntityClass() as KClass<IIdEntity<Any?>>
        return getHashCache(cacheName).getById(cacheName, id, entityClass)
    }

    /**
     * Centralizes the "look up Handler by cacheName" boilerplate: scans all beans of type
     * [AbstractHashCacheHandler] and indexes them by `cacheName()`. Previously each of the N callers
     * (evict / reload / reloadAll / getValue) repeated `SpringKit.getBeansOfType<...>().values.filter { ... }`
     * — iterating over all beans on every cache read/write is wasteful.
     *
     * The index is lazily built with double-checked locking: the first call scans once and groupBy is
     * stored in [handlerIndex], and subsequent calls do a direct map lookup. Newly added handler beans
     * will not be picked up automatically; the index is rebuilt by [resetForTesting] or by restarting
     * the context.
     */
    @Volatile private var handlerIndex: Map<String, List<AbstractHashCacheHandler<*>>>? = null

    private fun handlersFor(cacheName: String): List<AbstractHashCacheHandler<*>> {
        val index = handlerIndex ?: synchronized(this) {
            handlerIndex ?: SpringKit.getBeansOfType<AbstractHashCacheHandler<*>>().values
                .groupBy { it.cacheName() }
                .also { handlerIndex = it }
        }
        return index[cacheName].orEmpty()
    }

    /**
     * Returns the hash cache by name (an entity-id-keyed collection).
     *
     * @param cacheName cache name (logical name, resolved with the version prefix)
     * @return the IIdEntitiesHashCache for the given name
     * @throws IllegalStateException when MixHashCacheManager is unavailable, or the cacheName is not configured
     * (an entry with that cacheName must be added to the configuration)
     */
    fun getHashCache(cacheName: String): IHashCache {
        val manager = getManager()
            ?: throw IllegalStateException(
                "MixHashCacheManager is unavailable (caching is disabled, or the test does not extend a cache-enabling base class such as RdbAndRedisCacheTestBase)." +
                    " Check kudos.ability.cache.enabled or the test context."
            )
        return manager.getHashCache(cacheName)
            ?: throw IllegalStateException("Hash cache is not configured: add an entry named [$cacheName] in the sys_cache configuration table")
    }

    /**
     * Tells whether the given hash cache has the local layer enabled (i.e. retrieving the same key again
     * is guaranteed to return the same object reference).
     * Returns true only when the strategy is [CacheStrategy.LOCAL_REMOTE] or [CacheStrategy.SINGLE_LOCAL];
     * under [CacheStrategy.REMOTE] each read deserializes a fresh instance from remote storage, so it returns false.
     *
     * @param cacheName cache name (logical name, same as in [getHashCache])
     * @return false if not configured, or if not LOCAL_REMOTE/SINGLE_LOCAL
     */
    fun isLocalCacheEnabled(cacheName: String): Boolean {
        val strategy = getConfigProvider()?.getHashCacheConfigs()?.get(cacheName)?.resolvedStrategy ?: return false
        return strategy == CacheStrategy.LOCAL_REMOTE || strategy == CacheStrategy.SINGLE_LOCAL
    }

    // ---- Test injection hooks --------------------------------------------------
    // Same pattern as [KeyValueCacheKit]: defaults to SpringKit lookup; tests can override with mocks.
    // Note: type-scan-based Handler lookup (reload / reloadAll / getValue(no type)) still goes through Spring,
    //       because handler-by-cacheName matching only has clear semantics inside a real bean container; to test
    //       those paths, prefer starting Spring.

    @Volatile private var managerOverride: MixHashCacheManager? = null
    @Volatile private var configProviderOverride: ICacheConfigProvider? = null

    /**
     * 取 [MixHashCacheManager]：测试 override 优先；否则查 Spring bean。
     * @return manager 实例；缺失时返回 null（仅在 Spring 上下文未就绪时出现）
     * @author K
     * @since 1.0.0
     */
    private fun getManager(): MixHashCacheManager? =
        managerOverride ?: SpringKit.getBeanOrNull("mixHashCacheManager") as MixHashCacheManager?

    /**
     * 取 [ICacheConfigProvider]：测试 override 优先；否则查 Spring bean。
     * @return provider 实例；缺失时返回 null
     * @author K
     * @since 1.0.0
     */
    private fun getConfigProvider(): ICacheConfigProvider? =
        configProviderOverride ?: SpringKit.getBeanOrNull(ICacheConfigProvider::class)

    /**
     * Test-only: injects dependencies temporarily so unit tests do not need to start a full Spring context.
     * Passing null for any parameter falls back to the default [SpringKit] lookup path.
     * [resetForTesting] must be called at the end of the test to restore state.
     */
    fun overrideForTesting(
        manager: MixHashCacheManager? = null,
        configProvider: ICacheConfigProvider? = null,
    ) {
        managerOverride = manager
        configProviderOverride = configProvider
    }

    /**
     * Test-only: clears the mocks injected by [overrideForTesting] and restores Spring lookup.
     */
    fun resetForTesting() {
        managerOverride = null
        configProviderOverride = null
        handlerIndex = null
    }
}
