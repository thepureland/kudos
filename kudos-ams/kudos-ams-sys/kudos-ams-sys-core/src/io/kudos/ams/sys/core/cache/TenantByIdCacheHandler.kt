package io.kudos.ams.sys.core.cache

import io.kudos.ability.cache.common.batch.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ams.sys.core.dao.SysTenantDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 租户（by id）缓存处理器
 *
 * 1.数据来源表：sys_tenant
 * 2.缓存所有租户，包括active=false的
 * 3.缓存的key为：id
 * 4.缓存的value为：SysTenantCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class TenantByIdCacheHandler : AbstractByIdCacheHandler<String, SysTenantCacheItem, SysTenantDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_TENANT_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysTenantCacheItem? {
        return getSelf<TenantByIdCacheHandler>().getTenantById(key)
    }

    /**
     * 根据id从缓存中获取租户信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 租户id
     * @return SysTenantCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getTenantById(id: String): SysTenantCacheItem? {
        return getById(id)
    }

    /**
     * 根据多个id从缓存中批量获取租户信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 租户id集合
     * @return Map<租户id，SysTenantCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysTenantCacheItem::class
    )
    open fun getTenantsByIds(ids: Collection<String>): Map<String, SysTenantCacheItem> {
        return getByIds(ids)
    }

    override fun itemDesc() = "租户"

}