package io.kudos.ability.cache.interservice.client.core

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.Cache
import org.springframework.cache.get
import org.springframework.stereotype.Component

/**
 * Feign 跨服务缓存辅助器（client 侧）。
 *
 * 调用 Feign 远端服务时把响应缓存到本地（默认 TTL 600s），减少跨服务调用频次。
 * 没有本地 cache manager 时静默关闭——避免下游应用没引入 caffeine/redis 时启动失败。
 *
 * @author K
 * @since 1.0.0
 */
@Component
class ClientCacheHelper : InitializingBean {

    /** 本地缓存管理器；required=false 让缓存模块缺失时整个 helper 退化为 no-op */
    @Autowired(required = false)
    @Qualifier("localCacheManager")
    private val cacheManager: IKeyValueCacheManager<*>? = null

    /**
     * 判断是否存在本地缓存实现。
     * @return true 表示有本地 cache manager，可正常缓存
     * @author K
     * @since 1.0.0
     */
    fun hasLocalCache(): Boolean {
        return cacheManager != null
    }

    /**
     * 历史拼写遗留，已废弃，请用 [hasLocalCache]。
     * @author K
     * @since 1.0.0
     */
    @Deprecated("Use hasLocalCache()")
    fun havaLocalCache(): Boolean = hasLocalCache()

    /**
     * Spring 容器装配完毕后初始化 Feign 缓存空间。
     *
     * - 配置 `ignoreVersion = true`：跨服务缓存不参与版本前缀，避免上下游版本不同步导致 key miss
     * - 默认 TTL 600s 平衡 freshness 与缓存命中率
     *
     * @throws Exception 缓存初始化失败时由 Spring 接管
     * @author K
     * @since 1.0.0
     */
    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        if (!hasLocalCache()) {
            log.info("未找到本地缓存实现，功能不开启...")
            return
        }
        log.info("初始化Feign缓存空间...")
        val cacheName = ClientCacheKey.FEIGN_CACHE_PREFIX
        val cacheConfig = CacheConfig().apply {
            name = cacheName
            ignoreVersion = true
            ttl = 600
        }
        requireNotNull(cacheManager) { "localCacheManager not available" }
            .initCacheAfterSystemInit(mapOf(cacheName to cacheConfig))
        log.debug("初始化Feign缓存空间{0}完成", ClientCacheKey.FEIGN_CACHE_PREFIX)
    }

    /**
     * 加载本地缓存数据
     *
     * @param cacheKey cacheKey
     * @return Object
     */
    fun loadFromLocalCache(cacheKey: String): ClientCacheItem? {
        //可以考虑换成CacheKit
        return feignCache().get<ClientCacheItem>(cacheKey)
    }

    /**
     * 加载本地缓存数据
     *
     * @param cacheKey 本地缓存key值
     * @param data     缓存数据
     */
    fun writeToLocalCache(cacheKey: String, data: ClientCacheItem?) {
        //可以考虑换成CacheKit
        feignCache().put(cacheKey, data)
    }

    /**
     * 取 Feign 专用缓存区。
     * 缓存管理器或缓存区未初始化时立即抛错，避免后续 NPE 难定位。
     *
     * @return Feign 缓存区
     * @throws IllegalStateException 缓存未初始化时
     * @author K
     * @since 1.0.0
     */
    private fun feignCache(): Cache {
        val mgr = cacheManager ?: error("localCacheManager not available")
        return mgr.getCache(ClientCacheKey.FEIGN_CACHE_PREFIX)
            ?: error("Feign cache region ${ClientCacheKey.FEIGN_CACHE_PREFIX} not initialized")
    }

    /** 日志器 */
    private val log = LogFactory.getLog(this::class)

}
