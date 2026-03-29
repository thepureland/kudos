package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.base.tree.ListToTreeConverter
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.cache.SysMicroServiceHashCache
import io.kudos.ms.sys.core.dao.SysMicroServiceDao
import io.kudos.ms.sys.core.model.po.SysMicroService
import io.kudos.ms.sys.core.service.iservice.ISysMicroServiceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * 微服务业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysMicroServiceService(
    dao: SysMicroServiceDao
) : BaseCrudService<String, SysMicroService, SysMicroServiceDao>(dao),
    ISysMicroServiceService {

    private val log = LogFactory.getLog(this::class)

    @Autowired
    private lateinit var sysMicroServiceHashCache: SysMicroServiceHashCache

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysMicroServiceCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysMicroServiceHashCache.getMicroServiceByCode(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry? {
        return sysMicroServiceHashCache.getMicroServiceByCode(code)
    }

    override fun getFullMicroServiceTree(): List<IdAndNameTreeNode<String>> {
        val cacheEntries = getAllMicroServicesFromCache()
        val nodes = cacheEntries.map { IdAndNameTreeNode(it.code, it.name, it.parentCode) }
        return ListToTreeConverter.convert(nodes)
    }

    override fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceHashCache.getAllMicroServices()
    }

    override fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceHashCache.getMicroServicesByType(false)
    }

    override fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceHashCache.getMicroServicesByType(true)
    }

    override fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceHashCache.getAllMicroServices()
            .filter { it.parentCode == parentCode }
    }

    override fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceHashCache.getAllMicroServices()
            .filter { it.parentCode == parentCode && it.atomicService }
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
            sysMicroServiceHashCache.syncOnUpdate(microService, code)
        } else {
            log.error("更新编码为${code}的微服务的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        log.debug("新增编码为${code}的微服务。")
        sysMicroServiceHashCache.syncOnInsert(any, code) // 同步缓存
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val code = BeanKit.getProperty(any, SysMicroService::code.name) as String
        if (success) {
            log.debug("更新编码为${code}的微服务。")
            sysMicroServiceHashCache.syncOnUpdate(any, code)
        } else {
            log.error("更新编码为${code}的微服务失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val microService = dao.get(id)
        if (microService == null) {
            log.warn("删除编码为${id}的微服务时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除编码为${id}的微服务成功！")
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


}
