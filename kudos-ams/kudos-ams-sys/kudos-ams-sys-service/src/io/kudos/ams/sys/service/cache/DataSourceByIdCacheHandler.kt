package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ams.sys.service.dao.SysDataSourceDao
import org.soul.ability.cache.common.batch.BatchCacheable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 数据源缓存（by id）处理器
 *
 * 1. 缓存所有数据源，包括active=false的
 * 2. 缓存key为：id
 * 3. 缓存value为：SysDataSourceCacheItem
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class DataSourceByIdCacheHandler: AbstractByIdCacheHandler<String, SysDataSourceCacheItem, SysDataSourceDao>() {

    @Autowired
    private lateinit var self: DataSourceByIdCacheHandler

    companion object {
        private const val CACHE_NAME = "sys_data_source_by_id"
    }

    override fun itemDesc() = "数据源"

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): SysDataSourceCacheItem? {
        return self.getTenantById(key)
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getTenantById(id: String): SysDataSourceCacheItem? {
        return getById(id)
    }

    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysDataSourceCacheItem::class
    )
    open fun getTenantsByIds(ids: Collection<String>): Map<String, SysDataSourceCacheItem> {
        return getByIds(ids)
    }

}