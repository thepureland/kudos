package io.kudos.ams.user.provider.cache

import io.kudos.ability.cache.common.batch.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.user.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.user.provider.dao.AuthDeptDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 部门（by id）缓存处理器
 *
 * 1.数据来源表：auth_dept
 * 2.缓存所有部门，包括active=false的
 * 3.缓存的key为：id
 * 4.缓存的value为：AuthDeptCacheItem对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class DeptByIdCacheHandler : AbstractByIdCacheHandler<String, AuthDeptCacheItem, AuthDeptDao>() {

    companion object {
        private const val CACHE_NAME = "AUTH_DEPT_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): AuthDeptCacheItem? {
        return getSelf<DeptByIdCacheHandler>().getDeptById(key)
    }

    /**
     * 根据id从缓存中获取部门信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 部门id
     * @return AuthDeptCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#id",
        unless = "#result == null"
    )
    open fun getDeptById(id: String): AuthDeptCacheItem? {
        return getById(id)
    }

    /**
     * 根据多个id从缓存中批量获取部门信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 部门id集合
     * @return Map<部门id，AuthDeptCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = AuthDeptCacheItem::class
    )
    open fun getDeptsByIds(ids: Collection<String>): Map<String, AuthDeptCacheItem> {
        return getByIds(ids)
    }

    override fun itemDesc() = "部门"

}
