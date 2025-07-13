package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ams.sys.service.dao.SysTenantDao
import io.kudos.context.kit.SpringKit
import org.soul.ability.cache.common.batch.BatchCacheable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 租户（by id）缓存处理器
 *
 * 1.缓存所有租户
 * 2.缓存的key为：id
 * 3.缓存的value为：SysTenantCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class TenantByIdCacheHandler : AbstractByIdCacheHandler<String, SysTenantCacheItem, SysTenantDao>() {

    private var self: TenantByIdCacheHandler? = null
    
    companion object {
        const val CACHE_NAME = "SYS_TENANT_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysTenantCacheItem? {
        return getSelf().getTenantById(key)
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getTenantById(id: String): SysTenantCacheItem? {
        return getById(id)
    }

    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysTenantCacheItem::class
    )
    open fun getTenantsByIds(ids: Collection<String>): Map<String, SysTenantCacheItem> {
        return getByIds(ids)
    }

    private fun getSelf() : TenantByIdCacheHandler {
        if (self == null) {
            self = SpringKit.getBean(this::class)
        }
        return self!!
    }

    override fun itemDesc() = "租户"

}