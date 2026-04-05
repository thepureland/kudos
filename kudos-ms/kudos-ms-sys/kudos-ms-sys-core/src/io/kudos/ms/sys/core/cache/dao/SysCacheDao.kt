package io.kudos.ms.sys.core.cache.dao
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.cache.vo.SysCacheCacheEntry
import io.kudos.ms.sys.core.cache.model.po.SysCache
import io.kudos.ms.sys.core.cache.model.table.SysCaches
import org.springframework.stereotype.Repository


/**
 * 缓存数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysCacheDao : BaseCrudDao<String, SysCache, SysCaches>() {

    /**
     * 按原子服务编码、名称及启用状态查询单条缓存配置（供 Hash 缓存按 atomicServiceCode+name 回源）。
     *
     * @param atomicServiceCode 原子服务编码，非空
     * @param name 缓存名称，非空
     * @return 匹配的缓存项，不存在时 null
     */
    open fun fetchCacheEntryByNameAndAtomicServiceCode(atomicServiceCode: String, name: String): SysCacheCacheEntry? {
        val criteria = Criteria.and(
            SysCache::atomicServiceCode eq atomicServiceCode,
            SysCache::name eq name,
            SysCache::active eq true
        )
        return searchAs<SysCacheCacheEntry>(criteria).firstOrNull()
    }

    /**
     * 按原子服务编码查询缓存配置列表（供 Hash 缓存按 atomicServiceCode 回源）。
     *
     * @param atomicServiceCode 原子服务编码，非空
     * @return 匹配的缓存项列表
     */
    open fun fetchCachesByAtomicServiceCode(atomicServiceCode: String): List<SysCacheCacheEntry> {
        val criteria = Criteria(SysCache::atomicServiceCode eq atomicServiceCode)
        return searchAs<SysCacheCacheEntry>(criteria)
    }
}