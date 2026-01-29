package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.batch.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.provider.dao.AuthRoleDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 角色（by id）缓存处理器
 *
 * 1.数据来源表：auth_role
 * 2.缓存所有角色，包括active=false的
 * 3.缓存的key为：id
 * 4.缓存的value为：AuthRoleCacheItem对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class RoleByIdCacheHandler : AbstractByIdCacheHandler<String, AuthRoleCacheItem, AuthRoleDao>() {

    companion object {
        private const val CACHE_NAME = "AUTH_ROLE_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): AuthRoleCacheItem? {
        return getSelf<RoleByIdCacheHandler>().getRoleById(key)
    }

    /**
     * 根据id从缓存中获取角色信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 角色id
     * @return AuthRoleCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getRoleById(id: String): AuthRoleCacheItem? {
        return getById(id)
    }

    /**
     * 根据多个id从缓存中批量获取角色信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 角色id集合
     * @return Map<角色id，AuthRoleCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = AuthRoleCacheItem::class
    )
    open fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheItem> {
        return getByIds(ids)
    }

    override fun itemDesc() = "角色"

}
