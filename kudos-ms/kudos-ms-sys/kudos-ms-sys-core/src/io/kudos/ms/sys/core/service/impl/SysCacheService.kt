package io.kudos.ms.sys.core.service.impl

import io.kudos.ms.sys.common.vo.cache.SysCacheCacheItem
import io.kudos.ms.sys.common.vo.cache.SysCacheRecord
import io.kudos.ms.sys.common.vo.cache.SysCacheSearchPayload
import io.kudos.ms.sys.core.cache.CacheByNameCacheHandler
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import io.kudos.ms.sys.core.service.iservice.ISysCacheService
import io.kudos.ms.sys.core.model.po.SysCache
import io.kudos.ms.sys.core.dao.SysCacheDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 缓存业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysCacheService : BaseCrudService<String, SysCache, SysCacheDao>(), ISysCacheService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var cacheConfigCacheHandler: CacheByNameCacheHandler

    override fun getCacheFromCache(name: String): SysCacheCacheItem? {
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
        @Suppress("UNCHECKED_CAST")
        val names = dao.inSearchPropertyById(ids, SysCache::name.name) as List<String>
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
    override fun getCachesByAtomicServiceCode(atomicServiceCode: String): List<SysCacheRecord> {
        val searchPayload = SysCacheSearchPayload().apply {
            this.atomicServiceCode = atomicServiceCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysCacheRecord>
    }

    /**
     * 获取所有启用的缓存配置
     *
     * @return 缓存记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getAllActiveCaches(): List<SysCacheRecord> {
        val searchPayload = SysCacheSearchPayload().apply {
            this.active = true
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysCacheRecord>
    }

    //endregion your codes 2

}