package io.kudos.ms.sys.core.cache.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.cache.vo.SysCacheCacheEntry
import io.kudos.ms.sys.core.cache.model.po.SysCache
import io.kudos.ms.sys.core.cache.model.table.SysCaches
import org.springframework.stereotype.Repository


/**
 * Cache configuration DAO.
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysCacheDao : BaseCrudDao<String, SysCache, SysCaches>() {

    /**
     * Query a single cache configuration by atomic service code, name and active state (back-source for Hash cache by atomicServiceCode+name).
     *
     * @param atomicServiceCode atomic service code, non-blank
     * @param name cache name, non-blank
     * @return matching cache entry, or null if not found
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
     * Query the cache configuration list by atomic service code (back-source for Hash cache by atomicServiceCode).
     *
     * @param atomicServiceCode atomic service code, non-blank
     * @return list of matching cache entries
     */
    open fun fetchCachesByAtomicServiceCode(atomicServiceCode: String): List<SysCacheCacheEntry> {
        val criteria = Criteria(SysCache::atomicServiceCode eq atomicServiceCode)
        return searchAs<SysCacheCacheEntry>(criteria)
    }
}