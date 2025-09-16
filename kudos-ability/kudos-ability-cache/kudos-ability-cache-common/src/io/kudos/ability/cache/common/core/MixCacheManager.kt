package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.CacheItemInitializing
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.common.support.ICacheManager
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.support.AbstractCacheManager
import java.util.*

/**
 * 混合缓存(两级缓存: 本地+远程)管理器
 *
 * @author K
 * @since 1.0.0
 */
class MixCacheManager : AbstractCacheManager() {

    @Value("\${kudos.ability.cache.enabled}")
    val isCacheEnabled: Boolean? = null

    @Autowired
    private val versionConfig: CacheVersionConfig? = null

    @Autowired(required = false)
    @Qualifier("soulLocalCacheManager")
    private val localCacheManager: CacheManager? = null

    @Autowired(required = false)
    @Qualifier("soulRemoteCacheManager")
    private val remoteCacheManager: CacheManager? = null

    @Autowired
    private val cacheConfigProvider: ICacheConfigProvider? = null

    private val caches: MutableList<Cache?> = ArrayList<Cache?>()

    override fun loadCaches(): MutableCollection<out Cache?> {
        return caches
    }

    fun initCacheAfterSystemInit() {
        if (!this.isCacheEnabled!!) {
            log.warn("缓存未开启,不加载缓存配置.")
            return
        }
        if (localCacheManager == null && remoteCacheManager == null) {
            log.warn("无法找到缓存策略,不加载缓存配置.")
            return
        }
        //查询一次数据，各个缓存组件加载
        val localCacheConfigs = cacheConfigProvider!!.localCacheConfigs
        val remoteCacheConfigs = cacheConfigProvider.remoteCacheConfigs
        val localRemoteCacheConfigs = cacheConfigProvider.localRemoteCacheConfigs
        if (localCacheManager != null && localCacheManager is CacheItemInitializing) {
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

    override fun getCache(name: String): Cache? {
        val realName = versionConfig!!.getFinalCacheName(name)
        return super.getCache(realName)
    }

    /**
     * 加载本地缓存配置
     *
     * @return List<Cache>
    </Cache> */
    private fun loadLocalCacheConfig(localCacheConfigs: MutableMap<String, CacheConfig>): MutableList<Cache?> {
        val localCaches: MutableList<Cache?> = ArrayList<Cache?>()
        //本地缓存
        if (localCacheManager != null) {
            if (localCacheConfigs.isNotEmpty()) {
                localCacheConfigs.forEach { (key: String?, value: CacheConfig?) ->
                    val realKey = versionConfig!!.getFinalCacheName(key)
                    val localCache = localCacheManager.getCache(realKey)
                    localCaches.add(MixCache(CacheStrategy.SINGLE_LOCAL, localCache, null))
                }
            }
        } else {
            log.warn("找不到本地缓存策略，无法加载本地缓存配置！")
        }
        return localCaches
    }

    /**
     * 加载远程缓存配置
     *
     * @return remoteCaches
     */
    private fun loadRemoteCacheConfig(remoteCacheConfigs: MutableMap<String, CacheConfig>): MutableList<Cache?> {
        val remoteCaches: MutableList<Cache?> = ArrayList<Cache?>()
        //远程二级缓存
        if (remoteCacheManager != null) {
            if (remoteCacheConfigs.isNotEmpty()) {
                remoteCacheConfigs.forEach { (key: String?, value: CacheConfig?) ->
                    val realKey = versionConfig!!.getFinalCacheName(key)
                    val remoteCache = remoteCacheManager.getCache(realKey)
                    remoteCaches.add(MixCache(CacheStrategy.REMOTE, null, remoteCache))
                }
            }
        } else {
            log.warn("找不远程二级缓存策略，无法加载远程二级缓存配置！")
        }
        return remoteCaches
    }

    private fun loadMixCacheConfig(localRemoteCacheConfigs: MutableMap<String, CacheConfig>): MutableList<Cache?> {
        val mixCacheConfig: MutableList<Cache?> = ArrayList<Cache?>()
        // 本地-远程两级联动缓存
        if (localRemoteCacheConfigs.isNotEmpty()) {
            localRemoteCacheConfigs.forEach { (key: String?, value: CacheConfig?) ->
                val realKey = versionConfig!!.getFinalCacheName(key)
                val localCache = localCacheManager?.getCache(realKey)
                val remoteCache = remoteCacheManager?.getCache(realKey)
                lateinit var strategy: CacheStrategy
                if (localCacheManager == null) {
                    if (remoteCacheManager != null) {
                        strategy = CacheStrategy.REMOTE
                        log.warn("mix缓存,key={0}升级为远程缓存", key)
                    }
                } else {
                    if (remoteCacheManager != null) {
                        strategy = CacheStrategy.LOCAL_REMOTE
                    } else {
                        strategy = CacheStrategy.SINGLE_LOCAL
                        log.warn("mix缓存,key={0}降级为远程本地缓存", key)
                    }
                }
                mixCacheConfig.add(MixCache(strategy, localCache, remoteCache))
            }
        }
        return mixCacheConfig
    }

    /**
     * 清理本地缓存
     *
     * @param cacheName
     * @param key
     */
    fun clearLocal(cacheName: String, key: Any?) {
        val cache = super.getCache(cacheName)
        if (cache == null) {
            return
        }
        val mixCache = cache as MixCache
        if (key is String
            && key.endsWith("*")
            && localCacheManager != null
        ) {
            (localCacheManager as ICacheManager<*>).evictByPattern(cacheName, key)
        } else {
            mixCache.clearLocal(key)
        }
        log.debug("清除本地缓存：{0}::{1}", cacheName, Objects.toString(key, ""))
    }

    fun evictByPattern(cacheName: String, pattern: String) {
        val cache = getCache(cacheName)
        if (cache == null) {
            return
        }
        var patternKey = pattern
        if (!patternKey.endsWith("*")) {
            patternKey = "$patternKey*"
        }
        val mixCache = (cache as MixCache)
        if (mixCache.strategy == CacheStrategy.SINGLE_LOCAL) {
            (localCacheManager as ICacheManager<*>).evictByPattern(cacheName, patternKey)
        }
        if (mixCache.strategy == CacheStrategy.REMOTE) {
            (remoteCacheManager as ICacheManager<*>).evictByPattern(cacheName, patternKey)
        }
        if (mixCache.strategy == CacheStrategy.LOCAL_REMOTE) {
            (remoteCacheManager as ICacheManager<*>).evictByPattern(cacheName, patternKey)
            mixCache.pushMsgRedis(cache.getName(), patternKey)
        }
    }

    private val log = LogFactory.getLog(this)

}
