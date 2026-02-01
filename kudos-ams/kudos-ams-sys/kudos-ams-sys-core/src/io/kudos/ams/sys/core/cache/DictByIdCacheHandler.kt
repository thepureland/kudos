package io.kudos.ams.sys.core.cache

import io.kudos.ability.cache.common.batch.BatchCacheable
import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.dict.SysDictCacheItem
import io.kudos.ams.sys.core.dao.SysDictDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 字典基本信息缓存（by id）处理器
 *
 * 1.数据来源表：sys_dict
 * 2.缓存所有字典基本信息，包括active=false的
 * 3.缓存key为：id
 * 4.缓存value为：SysDictCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class DictByIdCacheHandler : AbstractByIdCacheHandler<String, SysDictCacheItem, SysDictDao>() {

    companion object {
        private const val CACHE_NAME = "SYS_DICT_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysDictCacheItem? {
        return getSelf<DictByIdCacheHandler>().getDictById(key)
    }

    /**
     * 根据id从缓存中获取字典基本信息，如果缓存中不存在，则从数据库中加载，并写入缓存
     *
     * @param dictId 字典id
     * @return SysDictCacheItem，找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#dictId",
        unless = "#result == null"
    )
    open fun getDictById(dictId: String): SysDictCacheItem? {
        return getById(dictId)
    }

    /**
     * 根据id集合批量从缓存中获取字典基本信息，缓存中不存在的，则从数据库中加载，并写入缓存
     *
     * @param ids 字典id集合
     * @return Map<字典id，SysDictCacheItem>，找不到返回空map
     */
    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysDictCacheItem::class
    )
    open fun getDictsByIds(ids: Collection<String>): Map<String, SysDictCacheItem> {
        return getByIds(ids)
    }

    override fun itemDesc() = "字典"

}