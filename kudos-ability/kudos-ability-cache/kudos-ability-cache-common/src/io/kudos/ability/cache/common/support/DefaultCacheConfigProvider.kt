package io.kudos.ability.cache.common.support

import org.soul.ability.cache.common.enums.CacheStrategy
import org.soul.ability.cache.common.starter.properties.CacheItemsProperties
import org.soul.ability.cache.common.support.CacheConfig
import org.soul.ability.cache.common.support.ICacheConfigProvider
import org.soul.base.bean.BeanTool
import org.soul.base.lang.collections.CollectionTool
import org.soul.base.lang.string.StringTool
import org.soul.base.log.Log
import org.soul.base.log.LogFactory

/**
 * 缓存配置默认提供者：约定从cache-config.yml文件配置
 */
class DefaultCacheConfigProvider(itemsProperties: CacheItemsProperties) : ICacheConfigProvider {
    init {
        val cacheItems = itemsProperties.getCacheItems()
        log.info("加载到cache-items配置：size=" + cacheItems.size)
        if (CollectionTool.isNotEmpty(cacheItems)) {
            initCacheConfig(cacheItems)
        }
    }

    /**
     * 从配置解析，初始化缓存
     *
     * @param cacheItems
     */
    private fun initCacheConfig(cacheItems: MutableList<String?>) {
        for (cacheItemStr in cacheItems) {
            if (StringTool.isNotBlank(cacheItemStr)) {
                val cacheConfig = cacheItemToConfig(cacheItemStr!!)
                if (!cacheConfigs.containsKey(cacheConfig.getStrategyDictCode())) {
                    cacheConfigs.put(cacheConfig.getStrategyDictCode(), HashMap<String?, CacheConfig?>())
                }
                cacheConfigs.get(cacheConfig.getStrategy())!!.put(cacheConfig.getName(), cacheConfig)
            }
        }
    }

    private fun cacheItemToConfig(cacheItem: String): CacheConfig {
        val config = CacheConfig()
        config.setWriteOnBoot(false)
        config.setActive(true)
        val params = cacheItem.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (param in params) {
            val item = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            BeanTool.copyProperty<CacheConfig?>(config, item[0], item[1])
        }
        return config
    }

    override fun getCacheConfig(name: String?): CacheConfig? {
        return getAllCacheConfigs().get(name)
    }

    override fun getAllCacheConfigs(): MutableMap<String?, CacheConfig?> {
        val result: MutableMap<String?, CacheConfig?> = HashMap<String?, CacheConfig?>()
        for (entry in cacheConfigs.entries) {
            for (entry1 in entry.value!!.entries) {
                result.put(entry1.key, entry1.value)
            }
        }
        return result
    }

    override fun getLocalCacheConfigs(): MutableMap<String?, CacheConfig?>? {
        return cacheConfigs.get(CacheStrategy.SINGLE_LOCAL.name)
    }

    override fun getRemoteCacheConfigs(): MutableMap<String?, CacheConfig?>? {
        return cacheConfigs.get(CacheStrategy.REMOTE.name)
    }

    override fun getLocalRemoteCacheConfigs(): MutableMap<String?, CacheConfig?>? {
        return cacheConfigs.get(CacheStrategy.LOCAL_REMOTE.name)
    }

    companion object {
        private val log: Log = LogFactory.getLog(DefaultCacheConfigProvider::class.java)

        /**
         * Map<Strategy></Strategy>, Map<CacheName></CacheName>, CacheConfig>>
         */
        private val cacheConfigs: MutableMap<String?, MutableMap<String?, CacheConfig?>?> =
            HashMap<String?, MutableMap<String?, CacheConfig?>?>()
    }
}
