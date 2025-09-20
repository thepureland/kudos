package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.batch.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.atomicservice.SysAtomicServiceCacheItem
import io.kudos.ams.sys.service.dao.SysAtomicServiceDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 原子服务（by code）缓存处理器
 *
 * 1.数据来源表：sys_atomic_service
 * 2.缓存所有原子服务，包括active=false的
 * 3.缓存的key为：code
 * 4.缓存的value为：SysAtomicServiceCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class AtomicServiceByCodeCacheHandler : AbstractByIdCacheHandler<String, SysAtomicServiceCacheItem, SysAtomicServiceDao>() {

    companion object Companion {
        private const val CACHE_NAME = "SYS_ATOMIC_SERVICE_BY_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysAtomicServiceCacheItem? {
        return getSelf<AtomicServiceByCodeCacheHandler>().getAtomicServiceByCode(key)
    }

    /**
     * 根据code从缓存中获取原子服务信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param code 原子服务code
     * @return SysAtomicServiceCacheItem, 打不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#code",
        unless = "#result == null"
    )
    open fun getAtomicServiceByCode(code: String): SysAtomicServiceCacheItem? {
        return getById(code)
    }

    /**
     * 根据多个code从缓存中批量获取原子服务信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param codes 原子服务code集合
     * @return Map<原子服务code，SysAtomicServiceCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysAtomicServiceCacheItem::class
    )
    open fun getAtomicServicesByCodes(codes: Collection<String>): Map<String, SysAtomicServiceCacheItem> {
        return getByIds(codes)
    }

    override fun itemDesc() = "原子服务"

}