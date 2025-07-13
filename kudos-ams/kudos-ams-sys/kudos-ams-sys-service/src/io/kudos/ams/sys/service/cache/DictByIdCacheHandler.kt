package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.support.AbstractByIdCacheHandler
import io.kudos.ams.sys.common.vo.dict.SysDictCacheItem
import io.kudos.ams.sys.service.dao.SysDictDao
import io.kudos.context.kit.SpringKit
import org.soul.ability.cache.common.batch.BatchCacheable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 字典基本信息缓存（by id）处理器
 *
 * 1. 缓存所有字典基本信息，包括active=true的
 * 2. 缓存key为：id
 * 3. 缓存value为：SysDictCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class DictByIdCacheHandler : AbstractByIdCacheHandler<String, SysDictCacheItem, SysDictDao>() {

    private var self: DictByIdCacheHandler? = null

    companion object {
        const val CACHE_NAME = "SYS_DICT_BY_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysDictCacheItem? {
        return getSelf().getDictById(key)
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#dictId",
        unless = "#result == null"
    )
    open fun getDictById(dictId: String): SysDictCacheItem? {
        return getById(dictId)
    }

    @BatchCacheable(
        cacheNames = [CACHE_NAME],
        valueClass = SysDictCacheItem::class
    )
    open fun getDictsByIds(ids: Collection<String>): Map<String, SysDictCacheItem> {
        return getByIds(ids)
    }

    private fun getSelf() : DictByIdCacheHandler {
        if (self == null) {
            self = SpringKit.getBean(this::class)
        }
        return self!!
    }

    override fun itemDesc() = "字典"

}