package io.kudos.ams.sys.provider.cache

import io.kudos.ability.cache.common.batch.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.subsystem.SysSubSystemCacheItem
import io.kudos.ams.sys.provider.dao.SysSubSystemDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 子系统（by code）缓存处理器
 *
 * 1.数据来源表：sys_sub_system
 * 2.缓存所有子系统，包括active=false的
 * 3.缓存的key为：code
 * 4.缓存的value为：SysSubSystemCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SubSystemByCodeCacheHandler : AbstractByIdCacheHandler<String, SysSubSystemCacheItem, SysSubSystemDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_SUB_SYSTEM_BY_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysSubSystemCacheItem? {
        return getSelf<SubSystemByCodeCacheHandler>().getSubSystemByCode(key)
    }

    /**
     * 根据code从缓存中获取子系统信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param code 子系统code
     * @return SysSubSystemCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#code",
        unless = "#result == null"
    )
    open fun getSubSystemByCode(code: String): SysSubSystemCacheItem? {
        return getById(code)
    }

    /**
     * 根据多个code从缓存中批量获取子系统信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param codes 子系统code集合
     * @return Map<子系统code，SysSubSystemCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysSubSystemCacheItem::class
    )
    open fun getSubSystemsByCodes(codes: Collection<String>): Map<String, SysSubSystemCacheItem> {
        return getByIds(codes)
    }

    override fun itemDesc() = "子系统"

}