package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.batch.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.dao.AuthUserDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户（by id）缓存处理器
 *
 * 1.数据来源表：auth_user
 * 2.缓存所有用户，包括active=false的
 * 3.缓存的key为：id
 * 4.缓存的value为：AuthUserCacheItem对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserByIdCacheHandler : AbstractByIdCacheHandler<String, AuthUserCacheItem, AuthUserDao>() {

    companion object {
        private const val CACHE_NAME = "AUTH_USER_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): AuthUserCacheItem? {
        return getSelf<UserByIdCacheHandler>().getUserById(key)
    }

    /**
     * 根据id从缓存中获取用户信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 用户id
     * @return AuthUserCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getUserById(id: String): AuthUserCacheItem? {
        return getById(id)
    }

    /**
     * 根据多个id从缓存中批量获取用户信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 用户id集合
     * @return Map<用户id，AuthUserCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = AuthUserCacheItem::class
    )
    open fun getUsersByIds(ids: Collection<String>): Map<String, AuthUserCacheItem> {
        return getByIds(ids)
    }

    override fun itemDesc() = "用户"

}
