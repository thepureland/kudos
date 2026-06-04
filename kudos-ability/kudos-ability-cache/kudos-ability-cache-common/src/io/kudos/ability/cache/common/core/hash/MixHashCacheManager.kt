package io.kudos.ability.cache.common.core.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.logger.LogFactory
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import io.kudos.base.lang.string.RandomStringKit

/**
 * Hash cache strategy wrapper manager: creates an [IHashCache] view (local / remote / two-tier) for each hash cache name based on configuration.
 * Consistent with the key-value [io.kudos.ability.cache.common.core.keyvalue.MixCacheManager]; supports the same three strategies.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
class MixHashCacheManager {

    @Value($$"${kudos.ability.cache.enabled:true}")
    val isCacheEnabled: Boolean = true

    @Resource
    private var versionConfig: CacheVersionConfig? = null

    @Autowired(required = false)
    @Qualifier("caffeineIdEntitiesHashCache")
    private var localHashCache: IHashCache? = null

    @Autowired(required = false)
    @Qualifier("redisIdEntitiesHashCache")
    private var remoteHashCache: IHashCache? = null

    @Resource
    private var cacheConfigProvider: ICacheConfigProvider? = null

    /**
     * When this matches the node ID used for Redis notifications, the receiver can filter out messages sent by this node. If no bean is provided, a random UUID is used.
     */
    @Autowired(required = false)
    @Qualifier("cacheNodeId")
    private var nodeId: String? = null

    private val hashCaches: MutableMap<String, IHashCache> = mutableMapOf()
    private val effectiveNodeId: String by lazy { nodeId ?: RandomStringKit.uuid() }

    /**
     * Invoked after system initialization completes; loads all hash cache configurations and creates the strategy wrapper views.
     */
    fun initHashCacheAfterSystemInit() {
        if (!isCacheEnabled) return
        val configProvider = cacheConfigProvider ?: return
        val configs = configProvider.getHashCacheConfigs()
        if (configs.isEmpty()) return
        val local = localHashCache
        val remote = remoteHashCache
        if (local == null && remote == null) {
            log.warn("No Hash cache implementation found (local/remote); Hash cache configuration will not be loaded.")
            return
        }
        val version = versionConfig ?: return
        configs.forEach { (name, config) ->
            val strategy = parseStrategy(config)
            val wrapper = MixHashCache(name, strategy, local, remote, effectiveNodeId)
            val realKey = version.getFinalCacheName(name)
            hashCaches[realKey] = wrapper
            log.debug("Initialized Hash cache [{0}] strategy={1}", name, strategy)
        }
    }

    /**
     * 取 [CacheConfig] 上配置的策略；未配置时默认 REMOTE（与 KV 缓存默认 LOCAL_REMOTE 不同——
     * hash 缓存数据量通常较大，全量本地化容易爆内存，所以默认只走远端）。
     *
     * @param config 缓存配置
     * @return 解析后的策略
     * @author K
     * @since 1.0.0
     */
    private fun parseStrategy(config: CacheConfig): CacheStrategy = config.resolvedStrategy ?: CacheStrategy.REMOTE

    fun getHashCache(cacheName: String): IHashCache? {
        val realName = versionConfig?.getFinalCacheName(cacheName) ?: cacheName
        return hashCaches[realName]
    }

    companion object {
        private val log = LogFactory.getLog(MixHashCacheManager::class)
    }
}