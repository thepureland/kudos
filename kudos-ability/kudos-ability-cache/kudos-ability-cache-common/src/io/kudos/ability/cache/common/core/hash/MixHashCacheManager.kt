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
import java.util.*

/**
 * Hash 缓存策略封装管理器：按配置为每个 hash 缓存名创建 [IHashCache] 视图（本地/远程/两级），
 * 与 key-value 的 [io.kudos.ability.cache.common.core.keyvalue.MixCacheManager] 一致，支持三种策略。
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
     * 与 Redis 通知用的节点 ID 一致时，收消息端可过滤本节点发出的消息。若未提供 bean 则使用随机 UUID。
     */
    @Autowired(required = false)
    @Qualifier("cacheNodeId")
    private var nodeId: String? = null

    private val hashCaches: MutableMap<String, IHashCache> = mutableMapOf()
    private val effectiveNodeId: String by lazy { nodeId ?: UUID.randomUUID().toString() }

    /**
     * 在系统初始化完成后调用，加载所有 hash 缓存配置并创建策略封装视图。
     */
    fun initHashCacheAfterSystemInit() {
        if (!isCacheEnabled) return
        val configProvider = cacheConfigProvider ?: return
        val configs = configProvider.getHashCacheConfigs()
        if (configs.isEmpty()) return
        val local = localHashCache
        val remote = remoteHashCache
        if (local == null && remote == null) {
            log.warn("未找到 Hash 缓存实现（local/remote），不加载 Hash 缓存配置")
            return
        }
        val version = versionConfig ?: return
        configs.forEach { (name, config) ->
            val strategy = parseStrategy(config)
            val wrapper = MixHashCache(name, strategy, local, remote, effectiveNodeId)
            val realKey = version.getFinalCacheName(name)
            hashCaches[realKey] = wrapper
            log.debug("初始化 Hash 缓存【{0}】策略={1}", name, strategy)
        }
    }

    private fun parseStrategy(config: CacheConfig): CacheStrategy {
        val s = config.strategy ?: config.strategyDictCode ?: return CacheStrategy.REMOTE
        return try {
            CacheStrategy.valueOf(s)
        } catch (_: Exception) {
            CacheStrategy.REMOTE
        }
    }

    fun getHashCache(cacheName: String): IHashCache? {
        val realName = versionConfig?.getFinalCacheName(cacheName) ?: cacheName
        return hashCaches[realName]
    }

    companion object {
        private val log = LogFactory.getLog(MixHashCacheManager::class)
    }
}