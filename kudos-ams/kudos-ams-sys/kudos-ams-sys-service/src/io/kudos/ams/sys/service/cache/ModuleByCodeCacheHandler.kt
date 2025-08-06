package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.module.SysModuleCacheItem
import io.kudos.ams.sys.service.dao.SysModuleDao
import org.soul.ability.cache.common.batch.BatchCacheable
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 模块（by code）缓存处理器
 *
 * 1.数据来源表：sys_module
 * 2.缓存所有模块，包括active=false的
 * 3.缓存的key为：code
 * 4.缓存的value为：SysModuleCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class ModuleByCodeCacheHandler : AbstractByIdCacheHandler<String, SysModuleCacheItem, SysModuleDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_MODULE_BY_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysModuleCacheItem? {
        return getSelf<ModuleByCodeCacheHandler>().getModuleByCode(key)
    }

    /**
     * 根据code从缓存中获取模块信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param code 模块code
     * @return SysModuleCacheItem, 找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#code",
        unless = "#result == null"
    )
    open fun getModuleByCode(code: String): SysModuleCacheItem? {
        return getById(code)
    }

    /**
     * 根据多个code从缓存中批量获取模块信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param codes 模块code集合
     * @return Map<模块code，SysModuleCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysModuleCacheItem::class
    )
    open fun getModulesByCodes(codes: Collection<String>): Map<String, SysModuleCacheItem> {
        return getByIds(codes)
    }

    override fun itemDesc() = "模块"

}