package io.kudos.ams.sys.service.cache

import io.kudos.ams.sys.common.vo.cache.SysCacheSearchPayload
import io.kudos.ams.sys.service.dao.SysCacheDao
import org.soul.ability.cache.common.enums.CacheStrategy
import org.soul.ability.cache.common.support.CacheConfig
import org.soul.ability.cache.common.support.ICacheConfigProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component


/**
 * 缓存配置提供者
 *
 * @author K
 * @since 1.0.0
 */
@Component
@DependsOn(value = ["dataSource", "springKit"])
open class CacheConfigProvider : ICacheConfigProvider {

    @Autowired
    private lateinit var sysCacheDao: SysCacheDao // 用ISysCacheBiz会级联引起CacheConfigCacheManager Bean的注册早于@Cacheable的扫描，造成该缓存失效！！！

    private var cacheConfigs: List<CacheConfig>? = null

    private fun getCacheConfigs(): List<CacheConfig> {
        if (cacheConfigs == null) {
            val searchPayload = SysCacheSearchPayload().apply {
                returnEntityClass = CacheConfig::class
                active = true
            }

            @Suppress("UNCHECKED_CAST")
            cacheConfigs = sysCacheDao.search(searchPayload) as List<CacheConfig>
        }
        return cacheConfigs ?: emptyList()
    }

    override fun getCacheConfig(name: String): CacheConfig? {
        return allCacheConfigs[name]
    }

    override fun getAllCacheConfigs(): Map<String, CacheConfig> {
        return getCacheConfigs().associateBy { it.name }
    }

    override fun getLocalCacheConfigs(): Map<String, CacheConfig> {
        return getCacheConfigs()
            .filter { it.strategyDictCode == CacheStrategy.SINGLE_LOCAL.name }
            .associateBy { it.name }
    }

    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> {
        return getCacheConfigs()
            .filter { it.strategyDictCode == CacheStrategy.REMOTE.name }
            .associateBy { it.name }
    }

    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> {
        return getCacheConfigs()
            .filter { it.strategyDictCode == CacheStrategy.LOCAL_REMOTE.name }
            .associateBy { it.name }
    }

}