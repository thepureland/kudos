package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.batch.keyvalue.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.core.dao.UserOrgDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 机构（by id）缓存处理器
 *
 * 1.数据来源表：user_org
 * 2.缓存所有机构，包括active=false的
 * 3.缓存的key为：id
 * 4.缓存的value为：UserOrgCacheItem对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class OrgByIdCacheHandler : AbstractByIdCacheHandler<String, UserOrgCacheItem, UserOrgDao>() {

    companion object {
        private const val CACHE_NAME = "USER_ORG_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): UserOrgCacheItem? {
        return getSelf<OrgByIdCacheHandler>().getOrgById(key)
    }

    /**
     * 根据id从缓存中获取机构信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 机构id
     * @return UserOrgCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getOrgById(id: String): UserOrgCacheItem? {
        return getById(id)
    }

    /**
     * 根据多个id从缓存中批量获取机构信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 机构id集合
     * @return Map<机构id，UserOrgCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = UserOrgCacheItem::class
    )
    open fun getOrgsByIds(ids: Collection<String>): Map<String, UserOrgCacheItem> {
        return getByIds(ids)
    }

    override fun itemDesc() = "机构"

}
