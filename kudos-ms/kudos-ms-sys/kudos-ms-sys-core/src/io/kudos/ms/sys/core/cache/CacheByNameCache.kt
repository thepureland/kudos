package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.cache.SysCacheCacheItem
import io.kudos.ms.sys.common.vo.cache.SysCacheSearchPayload
import io.kudos.ms.sys.core.dao.SysCacheDao
import io.kudos.ms.sys.core.model.po.SysCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 缓存配置信息的缓存处理器
 *
 * 1.数据来源表：sys_cache
 * 2.缓存所有缓存配置，不包括active=false的
 * 3.缓存key为：缓存name
 * 4.缓存value为：SysCacheCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class CacheByNameCache : AbstractKeyValueCacheHandler<SysCacheCacheItem>() {

    @Autowired
    private lateinit var sysCacheDao: SysCacheDao

    companion object {
        private const val CACHE_NAME = "SYS_CACHE_BY_NAME"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): SysCacheCacheItem? {
        return getSelf<CacheByNameCache>().getCache(key)
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有缓存配置信息！")
            return
        }

        // 加载所有可用的缓存配置
        val searchPayload = SysCacheSearchPayload().apply {
            returnEntityClass = SysCacheCacheItem::class
            active = true
        }

        @Suppress("UNCHECKED_CAST")
        val results = sysCacheDao.search(searchPayload, SysCacheCacheItem::class)
        log.debug("从数据库加载了${results.size}条缓存配置信息。")

        // 先清除缓存
        if (clear) {
            clear()
        }

        // 放入缓存
        results.forEach {
            val name = it.name ?: return@forEach
            CacheKit.put(cacheName(), name, it)
        }

        log.debug("缓存了${results.size}条缓存配置。")
    }

    /**
     * 根据名称从缓存中加载缓存配置信息，如果缓存中不存在，则从数据库加载，并写入缓存。
     *
     * @param name 缓存配置名称
     * @return SysCacheCacheItem，如果找不到返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#name",
        unless = "#result == null"
    )
    open fun getCache(name: String): SysCacheCacheItem? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在名称为${name}的缓存配置信息，从数据库中加载...")
        }
        val searchPayload = SysCacheSearchPayload().apply {
            returnEntityClass = SysCacheCacheItem::class
            this.name = name
            active = true
        }

        val result = sysCacheDao.search(searchPayload, SysCacheCacheItem::class).firstOrNull()
        if (result == null) {
            log.warn("数据库中不存在名称为${name}的缓存配置信息！")
        } else {
            log.debug("数据库加载到名称为${name}的缓存配置信息.")
        }
        return result
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 缓存配置id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${id}的缓存配置后，同步${CACHE_NAME}缓存...")
            val name = BeanKit.getProperty(any, SysCache::name.name) as String
            getSelf<CacheByNameCache>().getCache(name) // 缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 缓存配置id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的缓存配置后，同步${CACHE_NAME}缓存...")
            var name = BeanKit.getProperty(any, SysCache::name.name) as String?
            if (name == null) {
                name = sysCacheDao.get(id)?.name
            }
            val cacheName = name?.takeIf { it.isNotBlank() } ?: return
            CacheKit.evict(CACHE_NAME, cacheName) // 踢除缓存配置缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<CacheByNameCache>().getCache(cacheName) // 重新缓存
                log.debug("${CACHE_NAME}缓存同步完成。")
            }
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param id 缓存配置id
     * @param name 缓存配置名称
     */
    open fun syncOnDelete(id: String, name: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的缓存配置后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, name) // 踢除缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 缓存配置id集合
     * @param names 缓存配置名称列表
     */
    open fun syncOnBatchDelete(ids: Collection<String>, names: List<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的缓存配置后，同步从${CACHE_NAME}缓存中踢除...")
            names.forEach {
                CacheKit.evict(CACHE_NAME, it) // 踢除缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}