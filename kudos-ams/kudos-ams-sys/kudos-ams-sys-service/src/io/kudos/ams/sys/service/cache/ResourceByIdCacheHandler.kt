package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ams.sys.service.dao.SysResourceDao
import org.soul.ability.cache.common.batch.BatchCacheable
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 资源（by id）缓存处理器
 *
 * 1.缓存所有资源，包括active=false的
 * 2.缓存的key为：id
 * 3.缓存的value为：SysResourceCacheItem
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class ResourceByIdCacheHandler : AbstractByIdCacheHandler<String, SysResourceCacheItem, SysResourceDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_RESOURCE_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysResourceCacheItem? {
        return getSelf<ResourceByIdCacheHandler>().getResourceById(key)
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getResourceById(id: String): SysResourceCacheItem? {
        return getById(id)
    }

    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysResourceCacheItem::class
    )
    open fun getResourcesByIds(ids: Collection<String>): Map<String, SysResourceCacheItem> {
        return getByIds(ids)
    }

    override fun itemDesc() = "资源"

}