package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheItem
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceRecord
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceSearchPayload
import io.kudos.ms.sys.core.cache.SysMicroServiceHashCache
import io.kudos.ms.sys.core.dao.SysMicroServiceDao
import io.kudos.ms.sys.core.model.po.SysMicroService
import io.kudos.ms.sys.core.service.iservice.ISysMicroServiceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 微服务业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysMicroServiceService : BaseCrudService<String, SysMicroService, SysMicroServiceDao>(), ISysMicroServiceService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Resource
    private lateinit var sysMicroServiceHashCache: SysMicroServiceHashCache

    override fun getAllActiveMicroService(): List<SysMicroServiceCacheItem> {
        val atomic = sysMicroServiceHashCache.listAtomicServices()
        val nonAtomic = sysMicroServiceHashCache.getMicroServicesByType(false)
        return (atomic + nonAtomic).filter { it.active == true }
    }

    override fun getAllActiveMicroServiceExcludeAtomicService(): List<SysMicroServiceCacheItem> {
        return sysMicroServiceHashCache.getMicroServicesByType(false).filter { it.active == true }
    }

    override fun getAllActiveAtomicService(): List<SysMicroServiceCacheItem> {
        return sysMicroServiceHashCache.listAtomicServices().filter { it.active == true }
    }

    override fun getMicroServiceByCode(code: String): SysMicroServiceCacheItem? {
        return sysMicroServiceHashCache.getMicroServiceByCode(code)
    }

    override fun getAllActiveAtomicServiceByParentCode(parentCode: String): List<SysMicroServiceRecord> {
        val searchPayload = SysMicroServiceSearchPayload().apply {
            returnEntityClass = SysMicroServiceRecord::class
            active = true
            this.parentCode = parentCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload, SysMicroServiceRecord::class)
    }

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val microService = SysMicroService {
            this.code = code
            this.active = active
        }
        val success = dao.update(microService)
        if (success) {
            log.debug("更新编码为${code}的微服务的启用状态为${active}。")
            sysMicroServiceHashCache.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的微服务的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        log.debug("新增编码为${code}的微服务。")
        sysMicroServiceHashCache.syncOnInsert(code)
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val code = BeanKit.getProperty(any, SysMicroService::code.name) as String
        if (success) {
            log.debug("更新编码为${code}的微服务。")
            sysMicroServiceHashCache.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的微服务失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除编码为${id}的微服务。")
            sysMicroServiceHashCache.syncOnDelete(id)
        } else {
            log.error("删除编码为${id}的微服务失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除微服务，期望删除${ids.size}条，实际删除${count}条。")
        sysMicroServiceHashCache.syncOnBatchDelete(ids)
        return count
    }

    //endregion your codes 2

}