package io.kudos.ms.sys.core.microservice.service.impl
import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.base.tree.ListToTreeConverter
import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.cache.SysMicroServiceHashCache
import io.kudos.ms.sys.core.microservice.dao.SysMicroServiceDao
import io.kudos.ms.sys.core.microservice.model.po.SysMicroService
import io.kudos.ms.sys.core.microservice.service.iservice.ISysMicroServiceService
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
    dao: SysMicroServiceDao,
    private val sysMicroServiceHashCache: SysMicroServiceHashCache,
) : BaseCrudService<String, SysMicroService, SysMicroServiceDao>(dao),
    ISysMicroServiceService {

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysMicroServiceCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysMicroServiceHashCache.getMicroServiceByCode(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry? = sysMicroServiceHashCache.getMicroServiceByCode(code)

    override fun getFullMicroServiceTree(): List<IdAndNameTreeNode<String>> =
        ListToTreeConverter.convert(getAllMicroServicesFromCache().map(::toTreeNode))

    override fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry> = sysMicroServiceHashCache.getAllMicroServices()

    override fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry> =
        sysMicroServiceHashCache.getMicroServicesByType(false)

    override fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry> = sysMicroServiceHashCache.getMicroServicesByType(true)

    override fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry> =
        getAllMicroServicesFromCache().filter { it.parentCode == parentCode }

    override fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry> =
        getAllMicroServicesFromCache().filter { it.parentCode == parentCode && it.atomicService }

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val microService = SysMicroService {
            this.code = code
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(microService),
            log = log,
            successMessage = "更新编码为${code}的微服务的启用状态为${active}。",
            failureMessage = "更新编码为${code}的微服务的启用状态为${active}失败！",
        ) {
            sysMicroServiceHashCache.syncOnUpdate(microService, code)
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        completeCrudInsert(log, "新增编码为${code}的微服务。") {
            sysMicroServiceHashCache.syncOnInsert(any, code)
        }
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val code = requireMicroServiceCode(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新编码为${code}的微服务。",
            failureMessage = "更新编码为${code}的微服务失败！",
        ) {
            sysMicroServiceHashCache.syncOnUpdate(any, code)
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val microService = dao.get(id)
        if (microService == null) {
            log.warn("删除编码为${id}的微服务时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除编码为${id}的微服务成功！",
            failureMessage = "删除编码为${id}的微服务失败！",
        ) {
            sysMicroServiceHashCache.syncOnDelete(id)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除微服务，期望删除${ids.size}条，实际删除${count}条。")
        sysMicroServiceHashCache.syncOnBatchDelete(ids)
        return count
    }

    private fun toTreeNode(microService: SysMicroServiceCacheEntry): IdAndNameTreeNode<String> =
        IdAndNameTreeNode(microService.code, microService.name, microService.parentCode)

    private fun requireMicroServiceCode(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新微服务时不支持的入参类型: ${any::class.qualifiedName}")
}
