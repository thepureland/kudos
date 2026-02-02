package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.batch.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ms.sys.common.vo.system.SysSystemCacheItem
import io.kudos.ms.sys.core.dao.SysSystemDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 系统（by code）缓存处理器
 *
 * 1.数据来源表：sys_system
 * 2.缓存所有系统，包括active=false的
 * 3.缓存的key为：code
 * 4.缓存的value为：SysSystemCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SystemByCodeCacheHandler : AbstractByIdCacheHandler<String, SysSystemCacheItem, SysSystemDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_SYSTEM_BY_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysSystemCacheItem? {
        return getSelf<SystemByCodeCacheHandler>().getSystemByCode(key)
    }

    /**
     * 根据code从缓存中获取系统信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param code 系统code
     * @return SysSystemCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#code",
        unless = "#result == null"
    )
    open fun getSystemByCode(code: String): SysSystemCacheItem? {
        return getById(code)
    }

    /**
     * 根据多个code从缓存中批量获取系统信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param codes 系统code集合
     * @return Map<系统code，SysSystemCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysSystemCacheItem::class
    )
    open fun getSystemsByCodes(codes: Collection<String>): Map<String, SysSystemCacheItem> {
        return getByIds(codes)
    }

    override fun itemDesc() = "系统"

}
