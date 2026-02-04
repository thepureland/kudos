package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.batch.keyvalue.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.core.dao.UserAccountDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户（by id）缓存处理器
 *
 * 1.数据来源表：user_account
 * 2.缓存所有用户，包括active=false的
 * 3.缓存的key为：id
 * 4.缓存的value为：UserAccountCacheItem对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserByIdCache : AbstractByIdCacheHandler<String, UserAccountCacheItem, UserAccountDao>() {

    companion object {
        private const val CACHE_NAME = "USER_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): UserAccountCacheItem? {
        return getSelf<UserByIdCache>().getUserById(key)
    }

    /**
     * 根据id从缓存中获取用户信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 用户id
     * @return UserAccountCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getUserById(id: String): UserAccountCacheItem? {
        return getById(id)
    }

    /**
     * 根据多个id从缓存中批量获取用户信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 用户id集合
     * @return Map<用户id，UserAccountCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = UserAccountCacheItem::class
    )
    open fun getUsersByIds(ids: Collection<String>): Map<String, UserAccountCacheItem> {
        return getByIds(ids)
    }

    override fun itemDesc() = "用户"

}
