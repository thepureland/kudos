package io.kudos.ams.sys.service.biz.impl

import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import io.kudos.ams.sys.common.vo.cache.SysCacheCacheItem
import io.kudos.ams.sys.service.biz.ibiz.ISysCacheBiz
import io.kudos.ams.sys.service.cache.CacheByNameCacheHandler
import io.kudos.ams.sys.service.dao.SysCacheDao
import io.kudos.ams.sys.service.model.po.SysCache
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 缓存业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysCacheBiz : BaseCrudBiz<String, SysCache, SysCacheDao>(), ISysCacheBiz {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var cacheConfigCacheHandler: CacheByNameCacheHandler

    override fun getCacheFromCache(name: String): SysCacheCacheItem? {
        return cacheConfigCacheHandler.getCacheFromCache(name)
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
        cacheConfigCacheHandler.synchOnBatchDelete(ids, names)
        return count
    }

    //endregion your codes 2

}