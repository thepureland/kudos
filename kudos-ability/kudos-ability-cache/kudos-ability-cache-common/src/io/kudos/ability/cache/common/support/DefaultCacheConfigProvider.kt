package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties


/**
 * Default cache configuration provider: parses [CacheConfig] from the `kudos.cache.items` string-list configuration
 * (items shaped like `name=xxx&strategy=REMOTE&...`) and the `cache-item-configs` structured configuration.
 *
 * Key points:
 * - Configuration is parsed once during init and is immutable at runtime -> all lookups go against the flat map built after init, with O(1) hits.
 * - The legacy implementation kept cacheConfigs in a companion object (multiple instances would share state and tests would accumulate config across contexts); we now use instance fields.
 * - The legacy [getCacheConfig] and [getAllCacheConfigs] rebuilt the flat map on every call (O(strategies x cache items)),
 *   while Kit calls them repeatedly on hot paths. We now build once and read-only afterward to avoid repeated aggregation cost.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class DefaultCacheConfigProvider(itemsProperties: CacheItemsProperties) : ICacheConfigProvider {

    /** Configurations grouped by strategy (same structure as the legacy implementation, used only as construction-time data organization). */
    private val cacheConfigsByStrategy: MutableMap<String, MutableMap<String, CacheConfig>> = mutableMapOf()

    /** Flat full configuration map cacheName -> config, frozen after initialization. */
    private val flatConfigs: Map<String, CacheConfig>

    /** Pre-sliced immutable views per strategy, avoiding re-filtering on each lookup. */
    private val localConfigs: Map<String, CacheConfig>
    private val remoteConfigs: Map<String, CacheConfig>
    private val localRemoteConfigs: Map<String, CacheConfig>
    private val hashConfigs: Map<String, CacheConfig>

    init {
        val cacheItems = itemsProperties.cacheItems
        val cacheItemConfigs = itemsProperties.cacheItemConfigs
        log.info("Loaded cache-items config: size=${cacheItems.size}, cache-item-configs config: size=${cacheItemConfigs.size}")
        if (cacheItems.isNotEmpty()) {
            initCacheConfig(cacheItems)
        }
        if (cacheItemConfigs.isNotEmpty()) {
            initCacheConfigObjects(cacheItemConfigs)
        }

        flatConfigs = cacheConfigsByStrategy.values.flatMap { it.entries }
            .associate { it.key to it.value }
        localConfigs = (cacheConfigsByStrategy[CacheStrategy.SINGLE_LOCAL.name] ?: emptyMap()).toMap()
        remoteConfigs = (cacheConfigsByStrategy[CacheStrategy.REMOTE.name] ?: emptyMap()).toMap()
        localRemoteConfigs = (cacheConfigsByStrategy[CacheStrategy.LOCAL_REMOTE.name] ?: emptyMap()).toMap()
        hashConfigs = flatConfigs.filterValues { it.hash }
    }

    /**
     * Parses the configuration and initializes caches.
     *
     * @param cacheItems
     */
    private fun initCacheConfig(cacheItems: MutableList<String>) {
        cacheItems.filter { it.isNotBlank() }.forEach { cacheItemStr ->
            val cacheConfig = cacheItemToConfig(cacheItemStr)
            addCacheConfig(cacheConfig, cacheItemStr)
        }
    }

    private fun initCacheConfigObjects(cacheItemConfigs: MutableList<CacheConfig>) {
        cacheItemConfigs.forEachIndexed { index, cacheConfig ->
            addCacheConfig(normalizeDefaults(cacheConfig), "cache-item-configs[$index]")
        }
    }

    private fun cacheItemToConfig(cacheItem: String): CacheConfig {
        val config = CacheConfig().apply {
            writeOnBoot = false
            active = true
        }
        cacheItem.split("&").forEach { param ->
            val (key, value) = parseParam(param, cacheItem)
            require(key in writableConfigProperties) {
                "cache item contains unknown field '$key': $cacheItem; available fields: ${writableConfigProperties.joinToString()}"
            }
            BeanKit.setProperty<CacheConfig?>(config, key, value)
        }
        return config
    }

    private fun parseParam(param: String, cacheItem: String): Pair<String, String> {
        val pair = param.split("=", limit = 2)
        require(pair.size == 2 && pair[0].isNotBlank()) {
            "cache item parameter format is invalid; expected key=value: '$param' in '$cacheItem'"
        }
        return pair[0] to pair[1]
    }

    private fun normalizeDefaults(config: CacheConfig): CacheConfig = config.apply {
        if (writeOnBoot == null) {
            writeOnBoot = false
        }
        if (active == null) {
            active = true
        }
    }

    private fun addCacheConfig(cacheConfig: CacheConfig, source: String) {
        // Use the derived property `resolvedStrategyCode` instead of the raw `.strategy` — this is the final
        // remaining legacy reader to satisfy the README "new code must use the derived property" contract.
        // The yml parsing path only sets `.strategy`; the DB dictionary-code path only sets `.strategyDictCode`;
        // `resolvedStrategyCode` consolidates both sources in one place.
        val strategy = cacheConfig.resolvedStrategyCode
            ?: error("cache item is missing strategy: $source")
        require(cacheConfig.resolvedStrategy != null) {
            "cache item strategy is invalid '$strategy': $source; allowed values: ${CacheStrategy.values().joinToString { it.name }}"
        }
        val name = cacheConfig.name?.takeIf { it.isNotBlank() } ?: error("cache item is missing name: $source")
        cacheConfigsByStrategy.getOrPut(strategy) { mutableMapOf() }[name] = cacheConfig
    }

    override fun getCacheConfig(name: String): CacheConfig? = flatConfigs[name]

    override fun getAllCacheConfigs(): Map<String, CacheConfig> = flatConfigs

    override fun getLocalCacheConfigs(): Map<String, CacheConfig> = localConfigs

    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = remoteConfigs

    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = localRemoteConfigs

    override fun getHashCacheConfigs(): Map<String, CacheConfig> = hashConfigs

    companion object {
        private val log = LogFactory.getLog(this::class)
        private val writableConfigProperties: Set<String> = CacheConfig::class.memberProperties
            .filterIsInstance<KMutableProperty1<CacheConfig, *>>()
            .map { it.name }
            .toSortedSet()
    }
}
