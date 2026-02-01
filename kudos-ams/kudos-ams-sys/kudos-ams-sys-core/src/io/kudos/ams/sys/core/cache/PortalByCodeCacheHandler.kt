package io.kudos.ams.sys.core.cache

import io.kudos.ability.cache.common.batch.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.portal.SysPortalCacheItem
import io.kudos.ams.sys.core.dao.SysPortalDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 门户（by code）缓存处理器
 *
 * 1.数据来源表：sys_portal
 * 2.缓存所有门户，包括active=false的
 * 3.缓存的key为：code
 * 4.缓存的value为：SysPortalCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class PortalByCodeCacheHandler : AbstractByIdCacheHandler<String, SysPortalCacheItem, SysPortalDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_PORTAL_BY_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysPortalCacheItem? {
        return getSelf<PortalByCodeCacheHandler>().getPortalByCode(key)
    }

    /**
     * 根据code从缓存中获取门户信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param code 门户code
     * @return SysPortalCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#code",
        unless = "#result == null"
    )
    open fun getPortalByCode(code: String): SysPortalCacheItem? {
        return getById(code)
    }

    /**
     * 根据多个code从缓存中批量获取门户信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param codes 门户code集合
     * @return Map<门户code，SysPortalCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysPortalCacheItem::class
    )
    open fun getPortalsByCodes(codes: Collection<String>): Map<String, SysPortalCacheItem> {
        return getByIds(codes)
    }

    override fun itemDesc() = "门户"

}