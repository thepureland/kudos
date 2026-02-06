package io.kudos.ability.cache.common.support

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheItemsProperties
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory


/**
 * 缓存配置默认提供者：约定从cache-config.yml文件配置
 */
class DefaultCacheConfigProvider(itemsProperties: CacheItemsProperties) : ICacheConfigProvider {

    init {
        val cacheItems = itemsProperties.cacheItems
        log.info("加载到cache-items配置：size=" + cacheItems.size)
        if (cacheItems.isNotEmpty()) {
            initCacheConfig(cacheItems)
        }
    }

    /**
     * 从配置解析，初始化缓存
     *
     * @param cacheItems
     */
    private fun initCacheConfig(cacheItems: MutableList<String>) {
        for (cacheItemStr in cacheItems) {
            if (cacheItemStr.isNotBlank()) {
                val cacheConfig = cacheItemToConfig(cacheItemStr)
                if (!cacheConfigs.containsKey(cacheConfig.strategy)) {
                    cacheConfigs[cacheConfig.strategy!!] = HashMap()
                }
                cacheConfigs[cacheConfig.strategy]!![cacheConfig.name!!] = cacheConfig
            }
        }
    }

    private fun cacheItemToConfig(cacheItem: String): CacheConfig {
        val config = CacheConfig()
        config.writeOnBoot = false
        config.active = true
        val params = cacheItem.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (param in params) {
            val item = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            BeanKit.setProperty<CacheConfig?>(config, item[0], item[1])
        }
        return config
    }

    override fun getCacheConfig(name: String): CacheConfig? {
        return getAllCacheConfigs().get(name)
    }

    override fun getAllCacheConfigs(): Map<String, CacheConfig> {
        val result = mutableMapOf<String, CacheConfig>()
        for (entry in cacheConfigs.entries) {
            for (entry1 in entry.value.entries) {
                result[entry1.key] = entry1.value
            }
        }
        return result
    }

    override fun getLocalCacheConfigs(): Map<String, CacheConfig> {
        return cacheConfigs[CacheStrategy.SINGLE_LOCAL.name] ?: emptyMap()
    }

    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> {
       return cacheConfigs[CacheStrategy.REMOTE.name] ?: emptyMap()
    }

    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> {
       return cacheConfigs[CacheStrategy.LOCAL_REMOTE.name] ?: emptyMap()
    }

    override fun getHashCacheConfigs(): Map<String, CacheConfig> {
        return getAllCacheConfigs().filter { (_, config) -> config.hash == true }
    }

    companion object {
        private val log = LogFactory.getLog(this)

        /**
         * Map<Strategy></Strategy>, Map<CacheName></CacheName>, CacheConfig>>
         */
        private val cacheConfigs: MutableMap<String, MutableMap<String, CacheConfig>> = HashMap()
    }
}
