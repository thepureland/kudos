package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.sys.common.vo.cache.SysCacheCacheItem
import io.kudos.ams.sys.common.vo.cache.SysCacheSearchPayload
import io.kudos.ams.sys.service.dao.SysCacheDao
import io.kudos.ams.sys.service.model.po.SysCache
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 缓存配置信息的缓存处理器
 *
 * 1. 缓存所有缓存配置，包括active=false的
 * 2. 缓存key为：缓存name
 * 3. 缓存value为：SysCacheCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class CacheByNameCacheHandler : AbstractCacheHandler<SysCacheCacheItem>() {

    @Autowired
    private lateinit var sysCacheDao: SysCacheDao

    private var self: CacheByNameCacheHandler? = null

    companion object {
        private const val CACHE_NAME = "SYS_CACHE_BY_NAME"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysCacheCacheItem? {
        return getSelf().getCacheFromCache(key)
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有缓存配置信息！")
            return
        }

        // 加载所有可用的缓存配置
        val searchPayload = ListSearchPayload().apply {
            returnEntityClass = SysCacheCacheItem::class
        }

        @Suppress("UNCHECKED_CAST")
        val cacheConfigs = sysCacheDao.search(searchPayload) as List<SysCacheCacheItem>
        log.debug("从数据库加载了${cacheConfigs.size}条缓存配置信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存缓存配置
        cacheConfigs.forEach {
            CacheKit.putIfAbsent(CACHE_NAME, it.name!!, it)
        }
        log.debug("缓存了${cacheConfigs.size}条缓存配置。")
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#name",
        unless = "#result == null"
    )
    open fun getCacheFromCache(name: String): SysCacheCacheItem? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在名称为${name}的缓存配置信息，从数据库中加载...")
        }
        val searchPayload = SysCacheSearchPayload().apply {
            returnEntityClass = SysCacheCacheItem::class
            this.name = name
        }

        val result = sysCacheDao.search(searchPayload).firstOrNull() as SysCacheCacheItem?
        if (result == null) {
            log.warn("数据库中不存在名称为${name}的缓存配置信息！")
        } else {
            log.debug("数据库加载到名称为${name}的缓存配置信息.")
        }
        return result
    }

    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的缓存配置后，同步${CACHE_NAME}缓存...")
            val name = BeanKit.getProperty(any, SysCache::name.name) as String
            getSelf().getCacheFromCache(name) // 缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的缓存配置后，同步${CACHE_NAME}缓存...")
            var name = BeanKit.getProperty(any, SysCache::name.name) as String?
            if (name == null) {
                name = sysCacheDao.get(id)!!.name
            }
            CacheKit.evict(CACHE_NAME, name) // 踢除缓存配置缓存
            getSelf().getCacheFromCache(name) // 重新缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnDelete(id: String, name: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的缓存配置后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, name) // 踢除缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun synchOnBatchDelete(ids: Collection<String>, names: List<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的缓存配置后，同步从${CACHE_NAME}缓存中踢除...")
            names.forEach {
                CacheKit.evict(CACHE_NAME, it) // 踢除缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private fun getSelf() : CacheByNameCacheHandler {
        if (self == null) {
            self = SpringKit.getBean(this::class)
        }
        return self!!
    }

    private val log = LogFactory.getLog(this)

}