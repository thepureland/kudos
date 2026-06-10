package io.kudos.ability.cache.common.core.keyvalue

import io.kudos.ability.cache.common.core.CacheItemInitializing
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.support.AbstractCacheManager
import java.util.Objects

/**
 * Mixed cache manager.
 *
 * Provides unified management for two-tier caches (local + remote), automatically choosing a strategy based on configuration.
 *
 * Core responsibilities:
 * 1. Cache strategy management: supports SINGLE_LOCAL, REMOTE, and LOCAL_REMOTE strategies.
 * 2. Cache initialization: loads all configured caches after system initialization.
 * 3. Cache version management: supports cache version isolation via CacheVersionConfig.
 * 4. Pattern-based eviction: supports evicting cache entries by pattern (wildcard).
 *
 * Cache strategies:
 * - SINGLE_LOCAL: uses local cache only (e.g., Caffeine).
 * - REMOTE: uses remote cache only (e.g., Redis).
 * - LOCAL_REMOTE: two-tier cache; checks local first, falls back to remote on miss.
 *
 * Initialization flow:
 * 1. Check whether caching is enabled.
 * 2. Check whether a cache manager is present.
 * 3. Obtain three groups of configurations from the provider: local, remote, mixed.
 * 4. Initialize the local and remote cache managers.
 * 5. Load and register all cache instances.
 *
 * Cache downgrade/upgrade:
 * - If mixed cache is configured but the local manager is missing, downgrades to remote cache.
 * - If mixed cache is configured but the remote manager is missing, downgrades to local cache.
 * - Downgrade/upgrade events are logged automatically.
 *
 * Notes:
 * - initCacheAfterSystemInit must be invoked after system initialization completes.
 * - Cache names are automatically prefixed with the version.
 * - Pattern-based eviction may impact performance and should be used carefully.
 */
open class MixCacheManager : AbstractCacheManager() {

    @Value($$"${kudos.ability.cache.enabled}")
    val isCacheEnabled: Boolean? = null

    @Resource
    private val versionConfig: CacheVersionConfig? = null

//    @Autowired(required = false)
//    @Qualifier("localCacheManager")
    @Resource(name = "localCacheManager")
    private lateinit var localCacheManager: CacheManager

    @Autowired(required = false)
    @Qualifier("remoteCacheManager")
    private val remoteCacheManager: CacheManager? = null

    @Resource
    private val cacheConfigProvider: ICacheConfigProvider? = null

    private val caches: MutableList<Cache> = mutableListOf()

    /**
     * Determines whether `localCacheManager` was actually injected.
     * Uses `lateinit isInitialized` instead of a null check because it is injected via `@Resource(name = "localCacheManager")`;
     * when absent, Spring does not write null to the field but leaves it in an uninitialized state.
     *
     * @return true if the local cache manager exists
     * @author K
     * @since 1.0.0
     */
    private fun hasLocalCacheManager(): Boolean = this::localCacheManager.isInitialized

    /**
     * Spring `AbstractCacheManager` template method: returns all loaded cache instances.
     * [initCacheAfterSystemInit] populates [caches] in advance; this method simply returns it.
     *
     * @return cache collection
     * @author K
     * @since 1.0.0
     */
    override fun loadCaches(): Collection<Cache> {
        return caches
    }

    /**
     * Initializes all caches after system initialization completes.
     *
     * Loads all cache configurations from the provider, initializes the cache managers, and registers all cache instances.
     *
     * Workflow:
     * 1. Check whether caching is enabled: return immediately if not.
     * 2. Check cache managers: return immediately if neither local nor remote manager exists.
     * 3. Fetch cache configurations: obtain three groups from the provider:
     *    - localCacheConfigs: local-only cache configurations.
     *    - remoteCacheConfigs: remote-only cache configurations.
     *    - localRemoteCacheConfigs: mixed cache configurations.
     * 4. Initialize cache managers:
     *    - If the local manager supports initialization, pass in local + mixed configs.
     *    - If the remote manager supports initialization, pass in remote + mixed configs.
     * 5. Load cache instances:
     *    - Local cache instances.
     *    - Remote cache instances.
     *    - Mixed cache instances (may be downgraded).
     * 6. Complete initialization: call afterPropertiesSet to finish Spring cache manager setup.
     *
     * Configuration merging:
     * - Local manager receives: localCacheConfigs + localRemoteCacheConfigs.
     * - Remote manager receives: remoteCacheConfigs + localRemoteCacheConfigs.
     * - Mixed caches require both local and remote caches to be initialized.
     *
     * Invocation timing:
     * - After all Spring beans are initialized.
     * - Triggered by MixCacheInitializing.
     * - Ensures dependencies (e.g., database) are ready.
     *
     * Notes:
     * - Must be invoked after system initialization completes.
     * - If caching is disabled, no configuration will be loaded.
     * - Mixed caches may be downgraded due to missing managers.
     */
    fun initCacheAfterSystemInit() {
        if (this.isCacheEnabled != true) {
            log.warn("Caching is disabled; cache configuration will not be loaded.")
            return
        }
        if (!hasLocalCacheManager() && remoteCacheManager == null) {
            log.warn("No cache strategy found; cache configuration will not be loaded.")
            return
        }
        val provider = requireNotNull(cacheConfigProvider) { "Cache config provider is not injected; unable to load cache configuration." }
        // Query once so each cache component can load its data.
        val localCacheConfigs = provider.getLocalCacheConfigs()
        val remoteCacheConfigs = provider.getRemoteCacheConfigs()
        val localRemoteCacheConfigs = provider.getLocalRemoteCacheConfigs()
        if (hasLocalCacheManager() && localCacheManager is CacheItemInitializing) {
            (localCacheManager as CacheItemInitializing).initCacheAfterSystemInit(
                localCacheConfigs + localRemoteCacheConfigs
            )
        }
        if (remoteCacheManager != null && remoteCacheManager is CacheItemInitializing) {
            (remoteCacheManager as CacheItemInitializing).initCacheAfterSystemInit(
                remoteCacheConfigs + localRemoteCacheConfigs
            )
        }
        caches.addAll(loadLocalCacheConfig(localCacheConfigs))
        caches.addAll(loadRemoteCacheConfig(remoteCacheConfigs))
        caches.addAll(loadMixCacheConfig(localRemoteCacheConfigs))
        afterPropertiesSet()
    }

    /**
     * Overrides super's `getCache` to prefix the name with the version ([CacheVersionConfig.getFinalCacheName]) before lookup.
     * Callers may simply pass the logical name (without prefix) and need not handle versioning themselves.
     *
     * @param name logical cache name
     * @return the cache matched after applying the version prefix; null if missing
     * @author K
     * @since 1.0.0
     */
    open override fun getCache(name: String): Cache? {
        val realName = requireVersionConfig().getFinalCacheName(name)
        return super.getCache(realName)
    }

    /**
     * Loads local cache configurations.
     *
     * @return List<Cache>
    </Cache> */
    private fun loadLocalCacheConfig(localCacheConfigs: Map<String, CacheConfig>): MutableList<Cache> {
        val localCaches: MutableList<Cache> = mutableListOf()
        // local cache
        if (hasLocalCacheManager()) {
            if (localCacheConfigs.isNotEmpty()) {
                localCacheConfigs.forEach { (key: String, _: CacheConfig?) ->
                    val realKey = requireVersionConfig().getFinalCacheName(key)
                    val localCache = localCacheManager.getCache(realKey)
                    localCaches.add(MixCache(CacheStrategy.SINGLE_LOCAL, localCache, null))
                }
            }
        } else {
            log.warn("Local cache strategy not found; unable to load local cache configuration!")
        }
        return localCaches
    }

    /**
     * Loads remote cache configurations.
     *
     * @return remoteCaches
     */
    private fun loadRemoteCacheConfig(remoteCacheConfigs: Map<String, CacheConfig>): MutableList<Cache> {
        val remoteCaches: MutableList<Cache> = mutableListOf()
        // remote second-level cache
        if (remoteCacheManager != null) {
            if (remoteCacheConfigs.isNotEmpty()) {
                remoteCacheConfigs.forEach { (key: String, _: CacheConfig?) ->
                    val realKey = requireVersionConfig().getFinalCacheName(key)
                    val remoteCache = remoteCacheManager.getCache(realKey)
                    remoteCaches.add(MixCache(CacheStrategy.REMOTE, null, remoteCache))
                }
            }
        } else {
            log.warn("Remote second-level cache strategy not found; unable to load remote second-level cache configuration!")
        }
        return remoteCaches
    }

    /**
     * Loads mixed cache configurations.
     *
     * Loads the local-remote two-tier cache configuration, automatically downgrading or upgrading the strategy based on which cache managers are available.
     *
     * Workflow:
     * 1. Iterate all mixed cache configurations.
     * 2. Compute the final cache name (with version prefix).
     * 3. Try to obtain both local and remote cache instances.
     * 4. Determine strategy from available managers:
     *    - Local manager missing but remote present: upgrade to REMOTE strategy.
     *    - Remote manager missing but local present: downgrade to SINGLE_LOCAL strategy.
     *    - Both present: use LOCAL_REMOTE strategy.
     * 5. Create MixCache instances and add them to the list.
     *
     * Strategy selection:
     * - LOCAL_REMOTE: both local and remote managers present (ideal case).
     * - REMOTE: only the remote manager (local missing; auto-upgraded).
     * - SINGLE_LOCAL: only the local manager (remote missing; auto-downgraded).
     *
     * Downgrade/upgrade mechanism:
     * - Adapts to whichever cache managers are available.
     * - Logs downgrade/upgrade events for troubleshooting.
     * - Keeps caching functional even when configuration is incomplete.
     *
     * Notes:
     * - If neither manager exists, no cache instance is created.
     * - Downgrade/upgrade affects cache performance; complete configuration is recommended.
     * - Cache names are automatically prefixed with the version.
     *
     * @param localRemoteCacheConfigs mixed cache configuration map
     * @return list of mixed cache instances
     */
    private fun loadMixCacheConfig(localRemoteCacheConfigs: Map<String, CacheConfig>): MutableList<Cache> {
        val mixCacheConfig: MutableList<Cache> = mutableListOf()
        // local-remote two-tier cache
        if (localRemoteCacheConfigs.isNotEmpty()) {
            localRemoteCacheConfigs.forEach { (key: String, _: CacheConfig?) ->
                val realKey = requireVersionConfig().getFinalCacheName(key)
                val localCache = if (hasLocalCacheManager()) localCacheManager.getCache(realKey) else null
                val remoteCache = remoteCacheManager?.getCache(realKey)
                val strategy = if (!hasLocalCacheManager()) {
                    log.warn("mix cache, key={0} upgraded to remote cache", key)
                    CacheStrategy.REMOTE
                } else if (remoteCacheManager != null) {
                    CacheStrategy.LOCAL_REMOTE
                } else {
                    log.warn("mix cache, key={0} downgraded to local cache", key)
                    CacheStrategy.SINGLE_LOCAL
                }
                mixCacheConfig.add(MixCache(strategy, localCache, remoteCache))
            }
        }
        return mixCacheConfig
    }

    /**
     * Returns [versionConfig]; if not injected, throws immediately with a locatable error message.
     *
     * @return cache version configuration
     * @throws IllegalArgumentException when the config is not injected
     * @author K
     * @since 1.0.0
     */
    private fun requireVersionConfig(): CacheVersionConfig {
        return requireNotNull(versionConfig) { "Cache version config is not injected; unable to build cache name." }
    }

    /**
     * Clears the local cache.
     *
     * @param cacheName
     * @param key
     */
    fun clearLocal(cacheName: String, key: Any?) {
        // Callers may pass either a logical name (e.g., "test") or a name that already has the version prefix
        // (e.g., MixCache.getName() / names in distributed messages). The previous implementation called `getCache`
        // once (which auto-applies the prefix), `super.getCache` once (which does not), and then `evictByPattern(cacheName,...)`
        // with the un-normalized name. The three sites handled the prefix inconsistently: when called with a logical name,
        // evictByPattern could not find the prefixed Caffeine cache, turning pattern-eviction into a no-op. Normalize once
        // here and use realName everywhere downstream.
        val realName = requireVersionConfig().run {
            getFinalCacheName(getRealCacheName(cacheName))
        }
        val cache = super.getCache(realName) ?: return
        val mixCache = cache as MixCache
        if (key is String
            && key.endsWith("*")
            && hasLocalCacheManager()
        ) {
            (localCacheManager as IKeyValueCacheManager<*>).evictByPattern(realName, key)
        } else {
            mixCache.clearLocal(key)
        }
        log.debug("Evicted local cache: {0}::{1}", realName, Objects.toString(key, ""))
    }

    /**
     * Evicts cache entries by pattern.
     *
     * Evicts matching cache entries based on the cache strategy and pattern (wildcard supported).
     *
     * Workflow:
     * 1. Fetch the cache instance: get the MixCache instance by cache name.
     * 2. Normalize the pattern: append "*" if it does not already end with one.
     * 3. Evict by strategy:
     *    - SINGLE_LOCAL: evict by pattern in the local cache manager.
     *    - REMOTE: evict by pattern in the remote cache manager.
     *    - LOCAL_REMOTE: evict by pattern in the remote cache manager and push a Redis message to notify other nodes.
     *
     * Pattern matching:
     * - Supports the wildcard "*"; e.g., "user:*" matches all keys starting with "user:".
     * - If the pattern does not end with "*", one is appended for prefix matching.
     * - Pattern matching may impact performance and should be used carefully.
     *
     * Distributed synchronization:
     * - For the LOCAL_REMOTE strategy, a Redis message is pushed after evicting the remote cache.
     * - Other nodes evict their local cache upon receiving the message to maintain consistency.
     * - Notification is sent via pushMsgRedis.
     *
     * Notes:
     * - Pattern matching may require scanning all keys and incur significant overhead.
     * - For the LOCAL_REMOTE strategy, only the remote cache is evicted directly; local caches are evicted via the notification message.
     * - If the cache does not exist, returns immediately without action.
     *
     * @param cacheName cache name
     * @param pattern matching pattern; wildcard "*" supported
     */
    fun evictByPattern(cacheName: String, pattern: String) {
        val cache = getCache(cacheName) ?: return
        var patternKey = pattern
        if (!patternKey.endsWith("*")) {
            patternKey = "$patternKey*"
        }
        val mixCache = (cache as MixCache)
        // Naming contract differs between the two managers (see clearLocal): the local (Caffeine) manager
        // registers caches under the version-prefixed real name and expects it as-is, while the remote (Redis)
        // manager applies the version prefix internally and expects the logical name. Passing the logical name
        // to the local manager made pattern eviction a silent no-op whenever a cache version was configured.
        val localRealName = requireVersionConfig().getFinalCacheName(cacheName)
        when (mixCache.strategy) {
            CacheStrategy.SINGLE_LOCAL ->
                (localCacheManager as IKeyValueCacheManager<*>).evictByPattern(localRealName, patternKey)
            CacheStrategy.REMOTE ->
                (remoteCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, patternKey)
            CacheStrategy.LOCAL_REMOTE -> {
                (remoteCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, patternKey)
                // Evict this node's local tier explicitly: the broadcast below is filtered out on loopback by
                // nodeId (see RedisCacheMessageHandler.receiveMessage), so without this the local copy on the
                // evicting node would stay stale until TTL — inconsistent with the write-path contract
                // ("remote first, then local, then broadcast", see MixCache.writeThrough).
                if (hasLocalCacheManager()) {
                    (localCacheManager as IKeyValueCacheManager<*>).evictByPattern(localRealName, patternKey)
                }
                mixCache.pushMsgRedis(cache.getName(), patternKey)
            }
        }
    }

    /**
     * Checks whether the given key exists in the cache (independent of whether the value is null).
     * Delegates to the local/remote manager by strategy; under LOCAL_REMOTE, existence in either tier counts as present.
     *
     * @param cacheName cache name (logical name; the version prefix is applied internally)
     * @param key       cache key
     * @return true if present; false if absent or the cache is not configured
     */
    fun existsKey(cacheName: String, key: Any): Boolean {
        val realName = requireVersionConfig().getFinalCacheName(cacheName)
        val cache = super.getCache(realName) as? MixCache ?: return false
        return when (cache.strategy) {
            CacheStrategy.SINGLE_LOCAL -> (localCacheManager as IKeyValueCacheManager<*>).existsKey(realName, key)
            CacheStrategy.REMOTE -> (remoteCacheManager as IKeyValueCacheManager<*>).existsKey(realName, key)
            CacheStrategy.LOCAL_REMOTE -> (localCacheManager as IKeyValueCacheManager<*>).existsKey(realName, key) ||
                (remoteCacheManager as IKeyValueCacheManager<*>).existsKey(realName, key)
        }
    }

    /** Logger. */
    private val log = LogFactory.getLog(this::class)

}