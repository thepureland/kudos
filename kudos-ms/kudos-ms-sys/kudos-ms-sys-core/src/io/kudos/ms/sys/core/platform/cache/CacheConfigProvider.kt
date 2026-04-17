package io.kudos.ms.sys.core.platform.cache

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.cache.dao.SysCacheDao
import io.kudos.ms.sys.core.cache.model.po.SysCache
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
@DependsOn(value = ["dataSource"])
open class CacheConfigProvider : ICacheConfigProvider {

    @Autowired
    private lateinit var sysCacheDao: SysCacheDao // 用ISysCacheBiz会级联引起CacheConfigCacheManager Bean的注册早于@Cacheable的扫描，造成该缓存失效！！！

    private var cacheConfigs: List<CacheConfig>? = null

    private fun getCacheConfigs(): List<CacheConfig> {
        if (cacheConfigs == null) {
            val criteria = Criteria(SysCache::active eq true)
            cacheConfigs = sysCacheDao.searchAs<CacheConfig>(criteria)
        }
        return cacheConfigs ?: emptyList()
    }

    override fun getCacheConfig(name: String): CacheConfig? {
        return getAllCacheConfigs()[name]
    }

    override fun getAllCacheConfigs(): Map<String, CacheConfig> {
        return getCacheConfigs()
            .mapNotNull { cfg -> cfg.name?.let { it to cfg } }
            .toMap()
    }

    override fun getLocalCacheConfigs(): Map<String, CacheConfig> {
        return getCacheConfigs()
            .filter { it.strategyDictCode == CacheStrategy.SINGLE_LOCAL.name }
            .mapNotNull { cfg -> cfg.name?.let { it to cfg } }
            .toMap()
    }

    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> {
        return getCacheConfigs()
            .filter { it.strategyDictCode == CacheStrategy.REMOTE.name }
            .mapNotNull { cfg -> cfg.name?.let { it to cfg } }
            .toMap()
    }

    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> {
        return getCacheConfigs()
            .filter { it.strategyDictCode == CacheStrategy.LOCAL_REMOTE.name }
            .mapNotNull { cfg -> cfg.name?.let { it to cfg } }
            .toMap()
    }

    override fun getHashCacheConfigs(): Map<String, CacheConfig> {
        return getCacheConfigs()
            .filter { it.hash == true }
            .mapNotNull { cfg -> cfg.name?.let { it to cfg } }
            .toMap()
    }
}