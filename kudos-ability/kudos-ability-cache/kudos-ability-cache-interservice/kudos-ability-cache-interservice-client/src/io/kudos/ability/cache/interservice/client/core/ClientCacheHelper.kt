package io.kudos.ability.cache.interservice.client.core

import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheManager
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

/**
 * Feign缓存辅助器
 */
class ClientCacheHelper : InitializingBean {

    @Autowired(required = false)
    @Qualifier("localCacheManager")
    private val cacheManager: ICacheManager<*>? = null

    fun havaLocalCache(): Boolean {
        return cacheManager != null
    }

    /**
     * 初始化缓存空间
     */
    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        if (!havaLocalCache()) {
            log.info("未找到本地缓存实现，功能不开启...")
            return
        }
        log.info("初始化Feign缓存空间...")
        val cacheConfig = CacheConfig()
        cacheConfig.name = ClientCacheKey.FEIGN_CACHE_PREFIX
        cacheConfig.ignoreVersion = true
        cacheConfig.ttl = 600
        val cacheConfigMap = mutableMapOf<String, CacheConfig>()
        cacheConfigMap.put(cacheConfig.name!!, cacheConfig)
        cacheManager!!.initCacheAfterSystemInit(cacheConfigMap)
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
        return cacheManager!!.getCache(ClientCacheKey.FEIGN_CACHE_PREFIX)!!
            .get<ClientCacheItem?>(cacheKey, ClientCacheItem::class.java)
    }

    /**
     * 加载本地缓存数据
     *
     * @param cacheKey 本地缓存key值
     * @param data     缓存数据
     */
    fun writeToLocalCache(cacheKey: String, data: ClientCacheItem?) {
        //可以考虑换成CacheKit
        cacheManager!!.getCache(ClientCacheKey.FEIGN_CACHE_PREFIX)!!.put(cacheKey, data)
    }

    private val log = LogFactory.getLog(this)

}
