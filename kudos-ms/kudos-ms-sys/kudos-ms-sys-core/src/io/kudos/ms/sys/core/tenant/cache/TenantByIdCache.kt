package io.kudos.ms.sys.core.tenant.cache

import io.kudos.ability.cache.common.batch.keyvalue.BatchCacheable
import io.kudos.ability.cache.common.core.keyvalue.AbstractByIdCacheHandler
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 租户（by id）缓存处理器
 *
 * 1.数据来源表：sys_tenant
 * 2.缓存所有租户，包括active=false的
 * 3.缓存的key为：id
 * 4.缓存的value为：SysTenantCacheEntry对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class TenantByIdCache : AbstractByIdCacheHandler<String, SysTenantCacheEntry, SysTenantDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_TENANT_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysTenantCacheEntry? {
        return getSelf<TenantByIdCache>().getTenantById(key)
    }

    /**
     * 根据id从缓存中获取租户信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 租户id
     * @return SysTenantCacheEntry, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getTenantById(id: String): SysTenantCacheEntry? {
        return getById(id)
    }

    /**
     * 根据多个id从缓存中批量获取租户信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 租户id集合
     * @return Map<租户id，SysTenantCacheEntry>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysTenantCacheEntry::class
    )
    open fun getTenantsByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> {
        return getByIds(ids)
    }

    /**
     * 新增租户后同步（重载，接收业务对象与 id）。行为同单参 [syncOnInsert]。
     *
     * @param any 业务对象，仅用于重载区分
     * @param id 租户 id
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * 更新租户后同步（重载，带业务对象）。行为同单参 [syncOnUpdate]。
     */
    open fun syncOnUpdate(any: Any, id: String) {
        syncOnUpdate(id)
    }

    override fun itemDesc() = "租户"

}