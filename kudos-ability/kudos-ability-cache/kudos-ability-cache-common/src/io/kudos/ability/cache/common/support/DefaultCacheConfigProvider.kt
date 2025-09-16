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
                if (!cacheConfigs.containsKey(cacheConfig.strategyDictCode)) {
                    cacheConfigs.put(cacheConfig.strategyDictCode!!, HashMap())
                }
                cacheConfigs[cacheConfig.getStrategy()]!!.put(cacheConfig.name!!, cacheConfig)
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
            BeanKit.copyProperty<CacheConfig?>(config, item[0], item[1])
        }
        return config
    }

    override fun getCacheConfig(name: String): CacheConfig? {
        return allCacheConfigs[name]
    }



    override val allCacheConfigs: MutableMap<String, CacheConfig>
        get() {
            val result: MutableMap<String, CacheConfig> = HashMap()
            for (entry in cacheConfigs.entries) {
                for (entry1 in entry.value.entries) {
                    result.put(entry1.key, entry1.value)
                }
            }
            return result
        }

    override val localCacheConfigs: MutableMap<String, CacheConfig>
        get() = cacheConfigs[CacheStrategy.SINGLE_LOCAL.name]!!

    override val remoteCacheConfigs: MutableMap<String, CacheConfig>
        get() = cacheConfigs[CacheStrategy.REMOTE.name]!!

    override val localRemoteCacheConfigs: MutableMap<String, CacheConfig>
        get() = cacheConfigs[CacheStrategy.LOCAL_REMOTE.name]!!


    companion object {
        private val log = LogFactory.getLog(this)

        /**
         * Map<Strategy></Strategy>, Map<CacheName></CacheName>, CacheConfig>>
         */
        private val cacheConfigs: MutableMap<String, MutableMap<String, CacheConfig>> = HashMap()
    }
}
