package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.batch.keyvalue.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ms.auth.common.vo.group.AuthGroupCacheItem
import io.kudos.ms.auth.core.dao.AuthGroupDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户组（by id）缓存处理器
 *
 * 1.数据来源表：auth_group
 * 2.缓存所有用户组，包括active=false的
 * 3.缓存的key为：id
 * 4.缓存的value为：AuthGroupCacheItem对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class GroupByIdCache : AbstractByIdCacheHandler<String, AuthGroupCacheItem, AuthGroupDao>() {

    companion object {
        private const val CACHE_NAME = "AUTH_GROUP_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): AuthGroupCacheItem? {
        return getSelf<GroupByIdCache>().getGroupById(key)
    }

    /**
     * 根据id从缓存中获取用户组信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 用户组id
     * @return AuthGroupCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getGroupById(id: String): AuthGroupCacheItem? {
        return getById(id)
    }

    /**
     * 根据多个id从缓存中批量获取用户组信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 用户组id集合
     * @return Map<用户组id，AuthGroupCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = AuthGroupCacheItem::class
    )
    open fun getGroupsByIds(ids: Collection<String>): Map<String, AuthGroupCacheItem> {
        return getByIds(ids)
    }

    override fun itemDesc() = "用户组"

}
