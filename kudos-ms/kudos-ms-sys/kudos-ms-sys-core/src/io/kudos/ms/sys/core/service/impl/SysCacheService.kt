package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.data.json.JsonKit
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.cache.SysCacheCacheEntry
import io.kudos.ms.sys.common.vo.cache.SysCacheRow
import io.kudos.ms.sys.core.cache.CacheByNameCache
import io.kudos.ms.sys.core.dao.SysCacheDao
import io.kudos.ms.sys.core.model.po.SysCache
import io.kudos.ms.sys.core.service.iservice.ISysCacheService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 缓存业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
open class SysCacheService : BaseCrudService<String, SysCache, SysCacheDao>(), ISysCacheService {


    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var cacheConfigCacheHandler: CacheByNameCache

    override fun getCacheFromCache(name: String): SysCacheCacheEntry? {
        return cacheConfigCacheHandler.getCache(name)
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的缓存配置。")
        cacheConfigCacheHandler.syncOnInsert(any, id) // 同步缓存
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysCache::id.name) as String
        if (success) {
            log.debug("更新id为${id}的缓存配置。")
            cacheConfigCacheHandler.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的缓存配置失败！")
        }
        return success
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val cache = SysCache {
            this.id = id
            this.active = active
        }
        val success = dao.update(cache)
        if (success) {
            // 同步缓存
            cacheConfigCacheHandler.syncOnUpdate(cache, id)
        } else {
            log.error("更新id为${id}的缓存配置的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val sysCache = dao.get(id)
        if (sysCache == null) {
            log.warn("删除id为${id}的缓存配置时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的缓存配置成功！")
            cacheConfigCacheHandler.syncOnDelete(id, sysCache.name)
        } else {
            log.error("删除id为${id}的缓存配置失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val names = dao.inSearchPropertyById(ids, SysCache::name)
        val count = super.batchDelete(ids)
        log.debug("批量删除缓存配置，期望删除${ids.size}条，实际删除${count}条。")
        cacheConfigCacheHandler.syncOnBatchDelete(ids, names)
        return count
    }

    /**
     * 获取原子服务的缓存配置列表
     *
     * @param atomicServiceCode 原子服务编码
     * @return 缓存记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getCachesByAtomicServiceCode(atomicServiceCode: String): List<SysCacheRow> {
        val criteria = Criteria(SysCache::atomicServiceCode eq atomicServiceCode)
        return dao.searchAs<SysCacheRow>(criteria)
    }

    /**
     * 获取所有启用的缓存配置
     *
     * @return 缓存记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getAllActiveCaches(): List<SysCacheRow> {
        val criteria = Criteria(SysCache::active eq true)
        return dao.searchAs<SysCacheRow>(criteria)
    }

    override fun reload(name: String, key: String) {
        if (isKeyValueCache(name)) {
            KeyValueCacheKit.reload(name, key)
        } else {
            HashCacheKit.reload(name, key)
        }
    }

    override fun reloadAll(name: String) {
        if (isKeyValueCache(name)) {
            KeyValueCacheKit.reloadAll(name)
        } else {
            HashCacheKit.reloadAll(name)
        }
    }

    override fun evict(name: String, key: String) {
        if (isKeyValueCache(name)) {
            KeyValueCacheKit.evict(name, key)
        } else {
            HashCacheKit.evict(name, key)
        }
    }

    override fun evictAll(name: String) {
        if (isKeyValueCache(name)) {
            KeyValueCacheKit.clear(name)
        } else {
            HashCacheKit.clear(name)
        }
    }

    override fun existsKey(name: String, key: String): Boolean {
        return if (isKeyValueCache(name)) {
            KeyValueCacheKit.existsKey(name, key)
        } else {
            HashCacheKit.existsById(name, key)
        }
    }

    override fun getValueJson(name: String, key: String): String {
        val value = if (isKeyValueCache(name)) {
            KeyValueCacheKit.getValue(name, key)
        } else {
            HashCacheKit.getValue(name, key)
        }
        return JsonKit.toJson(value)
    }

    private fun isKeyValueCache(cacheName: String) : Boolean {
        val cache = cacheConfigCacheHandler.getCache(cacheName) ?: throw ServiceException("缓存【${cacheName}】不存在！")
        return cache.hash
    }


}