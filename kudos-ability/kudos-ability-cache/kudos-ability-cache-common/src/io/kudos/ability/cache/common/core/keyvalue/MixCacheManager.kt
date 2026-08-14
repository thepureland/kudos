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
        // Callers pass either a logical name ("test") or one that already carries the version prefix
        // (MixCache.getName(), names arriving in distributed messages). Normalize to both forms once: the
        // MixCache instances of this manager are registered under the real name, while the underlying managers
        // take the logical name per the IKeyValueCacheManager contract.
        val logicalName = requireVersionConfig().getRealCacheName(cacheName)
        val realName = requireVersionConfig().getFinalCacheName(logicalName)
        val cache = super.getCache(realName) ?: return
        val mixCache = cache as MixCache
        if (key is String
            && key.endsWith("*")
            && hasLocalCacheManager()
        ) {
            (localCacheManager as IKeyValueCacheManager<*>).evictByPattern(logicalName, key)
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
        // Both managers take the logical name and apply their own version prefix internally
        // (see the naming contract on IKeyValueCacheManager). This used to differ per implementation and was
        // compensated for here, which is what let the local tier be handed a prefixed pattern and match nothing.
        when (mixCache.strategy) {
            CacheStrategy.SINGLE_LOCAL ->
                (localCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, patternKey)
            CacheStrategy.REMOTE ->
                (remoteCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, patternKey)
            CacheStrategy.LOCAL_REMOTE -> {
                (remoteCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, patternKey)
                // Evict this node's local tier explicitly: the broadcast below is filtered out on loopback by
                // nodeId (see RedisCacheMessageHandler.receiveMessage), so without this the local copy on the
                // evicting node would stay stale until TTL — inconsistent with the write-path contract
                // ("remote first, then local, then broadcast", see MixCache.writeThrough).
                if (hasLocalCacheManager()) {
                    (localCacheManager as IKeyValueCacheManager<*>).evictByPattern(cacheName, patternKey)
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
        // The MixCache instances of this manager are registered under the real (version-prefixed) name, while
        // the underlying managers take the logical name — see the naming contract on IKeyValueCacheManager.
        val realName = requireVersionConfig().getFinalCacheName(cacheName)
        val cache = super.getCache(realName) as? MixCache ?: return false
        return when (cache.strategy) {
            CacheStrategy.SINGLE_LOCAL -> (localCacheManager as IKeyValueCacheManager<*>).existsKey(cacheName, key)
            CacheStrategy.REMOTE -> (remoteCacheManager as IKeyValueCacheManager<*>).existsKey(cacheName, key)
            CacheStrategy.LOCAL_REMOTE -> (localCacheManager as IKeyValueCacheManager<*>).existsKey(cacheName, key) ||
                (remoteCacheManager as IKeyValueCacheManager<*>).existsKey(cacheName, key)
        }
    }

    /**
     * Looks up many keys in one go, honouring the cache strategy.
     *
     * Under LOCAL_REMOTE the local tier is asked first and only the keys it missed are fetched from remote,
     * which are then backfilled locally — mirroring [MixCache.mixGet]'s single-key behaviour, but collapsing
     * what used to be one remote round trip per key into one per batch.
     *
     * **A key present in the result was a cache hit, even if its value is null**; see
     * [IKeyValueCacheManager.multiGet]. Callers must branch on `containsKey`.
     *
     * @param cacheName logical cache name
     * @param keys      keys to look up
     * @return found keys mapped to their (possibly null) values, in the order given
     */
    fun multiGet(cacheName: String, keys: Collection<Any>): Map<Any, Any?> {
        if (keys.isEmpty()) return emptyMap()
        // getCache (this class's override) applies the version prefix itself, same as evictByPattern does.
        val cache = getCache(cacheName) ?: return emptyMap()
        // Anything that is not a MixCache (or a MixCache whose tier managers cannot batch) is still read
        // correctly, one key at a time, through the plain Cache API. Bulk lookup is an optimisation; failing
        // to apply it must not turn every key into a miss and send the whole batch back to the source.
        val mixCache = cache as? MixCache ?: return readEachThroughCache(cache, keys)
        val local = localCacheManagerOrNull()
        val remote = remoteCacheManager as? IKeyValueCacheManager<*>
        return when (mixCache.strategy) {
            CacheStrategy.SINGLE_LOCAL -> local?.multiGet(cacheName, keys) ?: readEachThroughCache(cache, keys)
            CacheStrategy.REMOTE -> remote?.multiGet(cacheName, keys) ?: readEachThroughCache(cache, keys)
            CacheStrategy.LOCAL_REMOTE -> {
                if (local == null || remote == null) return readEachThroughCache(cache, keys)
                val fromLocal = local.multiGet(cacheName, keys)
                val missing = keys.filterNot { fromLocal.containsKey(it) }
                if (missing.isEmpty()) return fromLocal
                val fromRemote = remote.multiGet(cacheName, missing)
                // Backfill so the next batch is served locally, preserving cached nulls.
                val localCache = mixCache.localCacheOrNull()
                if (localCache != null) {
                    fromRemote.forEach { (k, v) ->
                        runCatching { localCache.put(k, v) }.onFailure {
                            log.warn("Backfilling local cache failed cacheName={0} key={1} cause={2}", cacheName, k, it.message)
                        }
                    }
                }
                // Rebuild in the caller's key order rather than concatenating the two maps.
                val merged = LinkedHashMap<Any, Any?>(fromLocal.size + fromRemote.size)
                keys.forEach { key ->
                    when {
                        fromLocal.containsKey(key) -> merged[key] = fromLocal[key]
                        fromRemote.containsKey(key) -> merged[key] = fromRemote[key]
                    }
                }
                merged
            }
        }
    }

    /** The local manager as an [IKeyValueCacheManager], or null when no local tier is wired. */
    private fun localCacheManagerOrNull(): IKeyValueCacheManager<*>? =
        if (hasLocalCacheManager()) localCacheManager as? IKeyValueCacheManager<*> else null

    /**
     * Correct-but-unbatched fallback for [multiGet]: reads each key through the plain `Cache` API.
     *
     * Keeps the "present key = hit" contract, cached nulls included, so callers cannot tell the difference
     * apart from the number of round trips.
     */
    private fun readEachThroughCache(cache: Cache, keys: Collection<Any>): Map<Any, Any?> {
        val result = LinkedHashMap<Any, Any?>()
        keys.forEach { key -> cache.get(key)?.let { result[key] = it.get() } }
        return result
    }

    /** Logger. */
    private val log = LogFactory.getLog(this::class)

}