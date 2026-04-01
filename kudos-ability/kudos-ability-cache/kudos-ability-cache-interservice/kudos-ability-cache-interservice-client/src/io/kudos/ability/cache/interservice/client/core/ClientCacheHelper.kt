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
 * Feign缓存辅助器
 */
@Component
class ClientCacheHelper : InitializingBean {

    @Autowired(required = false)
    @Qualifier("localCacheManager")
    private val cacheManager: IKeyValueCacheManager<*>? = null

    fun hasLocalCache(): Boolean {
        return cacheManager != null
    }

    @Deprecated("Use hasLocalCache()")
    fun havaLocalCache(): Boolean = hasLocalCache()

    /**
     * 初始化缓存空间
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

    private fun feignCache(): Cache {
        val mgr = cacheManager ?: error("localCacheManager not available")
        return mgr.getCache(ClientCacheKey.FEIGN_CACHE_PREFIX)
            ?: error("Feign cache region ${ClientCacheKey.FEIGN_CACHE_PREFIX} not initialized")
    }

    private val log = LogFactory.getLog(this::class)

}
