package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.microservice.SysMicroServiceCacheItem
import io.kudos.ams.sys.service.dao.SysMicroServiceDao
import org.soul.ability.cache.common.batch.BatchCacheable
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 微服务（by code）缓存处理器
 *
 * 1.数据来源表：sys_micro_service
 * 2.缓存所有微服务，包括active=false的
 * 3.缓存的key为：code
 * 4.缓存的value为：SysMicroServiceCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class MicroServiceByCodeCacheHandler : AbstractByIdCacheHandler<String, SysMicroServiceCacheItem, SysMicroServiceDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_MICRO_SERVICE_BY_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysMicroServiceCacheItem? {
        return getSelf<MicroServiceByCodeCacheHandler>().getMicroServiceByCode(key)
    }

    /**
     * 根据code从缓存中获取微服务信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param code 微服务code
     * @return SysMicroServiceCacheItem, 打不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#code",
        unless = "#result == null"
    )
    open fun getMicroServiceByCode(code: String): SysMicroServiceCacheItem? {
        return getById(code)
    }

    /**
     * 根据多个code从缓存中批量获取微服务信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param codes 微服务code集合
     * @return Map<微服务code，SysMicroServiceCacheItem>
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysMicroServiceCacheItem::class
    )
    open fun getMicroServicesByCodes(codes: Collection<String>): Map<String, SysMicroServiceCacheItem> {
        return getByIds(codes)
    }

    override fun itemDesc() = "微服务"

}