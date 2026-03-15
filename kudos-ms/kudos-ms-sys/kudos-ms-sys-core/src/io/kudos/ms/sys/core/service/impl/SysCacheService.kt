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
import io.kudos.ms.sys.core.cache.SysCacheHashCache
import io.kudos.ms.sys.core.dao.SysCacheDao
import io.kudos.ms.sys.core.model.po.SysCache
import io.kudos.ms.sys.core.service.iservice.ISysCacheService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


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
    private lateinit var sysCacheHashCache: SysCacheHashCache

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysCacheCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysCacheHashCache.getCacheById(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getCacheFromCache(id: String): SysCacheCacheEntry? {
        return sysCacheHashCache.getCacheById(id)
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的缓存配置。")
        sysCacheHashCache.syncOnInsert(any, id) // 同步 Hash 缓存
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysCache::id.name) as String
        if (success) {
            log.debug("更新id为${id}的缓存配置。")
            sysCacheHashCache.syncOnUpdate(any, id)
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
            sysCacheHashCache.syncOnUpdate(cache, id)
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
            sysCacheHashCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的缓存配置失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除缓存配置，期望删除${ids.size}条，实际删除${count}条。")
        sysCacheHashCache.syncOnBatchDelete(ids)
        return count
    }

    /**
     * 获取原子服务的缓存配置列表
     *
     * @param atomicServiceCode 原子服务编码
     * @return 缓存记录列表
     */
    override fun getCachesFromCache(atomicServiceCode: String): List<SysCacheCacheEntry> {
        return sysCacheHashCache.getCaches(atomicServiceCode)
    }

    override fun reload(id: String, key: String) {
        val cache = getCacheConfigById(id)
        val name = cache.name
        if (isKeyValueCache(cache)) {
            KeyValueCacheKit.reload(name, key)
        } else {
            HashCacheKit.reload(name, key)
        }
    }

    override fun reloadAll(id: String) {
        val cache = getCacheConfigById(id)
        val name = cache.name
        if (isKeyValueCache(cache)) {
            KeyValueCacheKit.reloadAll(name)
        } else {
            HashCacheKit.reloadAll(name)
        }
    }

    override fun evict(id: String, key: String) {
        val cache = getCacheConfigById(id)
        val name = cache.name
        if (isKeyValueCache(cache)) {
            KeyValueCacheKit.evict(name, key)
        } else {
            HashCacheKit.evict(name, key)
        }
    }

    override fun evictAll(id: String) {
        val cache = getCacheConfigById(id)
        val name = cache.name
        if (isKeyValueCache(cache)) {
            KeyValueCacheKit.clear(name)
        } else {
            HashCacheKit.clear(name)
        }
    }

    override fun existsKey(id: String, key: String): Boolean {
        val cache = getCacheConfigById(id)
        val name = cache.name
        return if (isKeyValueCache(cache)) {
            KeyValueCacheKit.existsKey(name, key)
        } else {
            HashCacheKit.existsById(name, key)
        }
    }

    override fun getValueJson(id: String, key: String): String {
        val cache = getCacheConfigById(id)
        val name = cache.name
        val value = if (isKeyValueCache(cache)) {
            KeyValueCacheKit.getValue(name, key)
        } else {
            HashCacheKit.getValue(name, key)
        }
        return JsonKit.toJson(value)
    }

    /** 按 id 获取缓存配置，不存在则抛异常 */
    private fun getCacheConfigById(id: String): SysCacheCacheEntry {
        return sysCacheHashCache.getCacheById(id) ?: throw ServiceException("缓存配置【$id】不存在！")
    }

    private fun isKeyValueCache(cache: SysCacheCacheEntry): Boolean = !cache.hash


}