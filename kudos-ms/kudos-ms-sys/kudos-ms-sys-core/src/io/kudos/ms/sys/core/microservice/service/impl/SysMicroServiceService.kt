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
import io.kudos.ms.sys.core.microservice.event.SysMicroServiceBatchDeleted
import io.kudos.ms.sys.core.microservice.event.SysMicroServiceDeleted
import io.kudos.ms.sys.core.microservice.event.SysMicroServiceInserted
import io.kudos.ms.sys.core.microservice.event.SysMicroServiceUpdated
import io.kudos.ms.sys.core.microservice.model.po.SysMicroService
import io.kudos.ms.sys.core.microservice.service.iservice.ISysMicroServiceService
import org.springframework.context.ApplicationEventPublisher
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
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysMicroService, SysMicroServiceDao>(dao),
    ISysMicroServiceService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysMicroServiceCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysMicroServiceHashCache.getMicroServiceByCode(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    @Transactional(readOnly = true)
    override fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry? = sysMicroServiceHashCache.getMicroServiceByCode(code)

    @Transactional(readOnly = true)
    override fun getFullMicroServiceTree(): List<IdAndNameTreeNode<String>> =
        ListToTreeConverter.convert(getAllMicroServicesFromCache().map(::toTreeNode))

    @Transactional(readOnly = true)
    override fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry> = sysMicroServiceHashCache.getAllMicroServices()

    @Transactional(readOnly = true)
    override fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry> =
        sysMicroServiceHashCache.getMicroServicesByType(false)

    @Transactional(readOnly = true)
    override fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry> = sysMicroServiceHashCache.getMicroServicesByType(true)

    @Transactional(readOnly = true)
    override fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry> =
        getAllMicroServicesFromCache().filter { it.parentCode == parentCode }

    @Transactional(readOnly = true)
    override fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry> =
        getAllMicroServicesFromCache().filter { it.parentCode == parentCode && it.atomicService }

    @Transactional(readOnly = true)
    override fun getActiveAtomicServiceCodes(): List<String> =
        getAtomicServicesFromCache().filter { it.active }.map { it.code }

    @Transactional(readOnly = true)
    override fun getActiveMicroServiceCodes(): List<String> =
        getMicroServicesExcludeAtomicFromCache().filter { it.active }.map { it.code }

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
            eventPublisher.publishEvent(SysMicroServiceUpdated(id = code))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        completeCrudInsert(log, "新增编码为${code}的微服务。") {
            eventPublisher.publishEvent(SysMicroServiceInserted(id = code))
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
            eventPublisher.publishEvent(SysMicroServiceUpdated(id = code))
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
            eventPublisher.publishEvent(SysMicroServiceDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除微服务，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0) {
            eventPublisher.publishEvent(SysMicroServiceBatchDeleted(ids = ids))
        }
        return count
    }

    /**
     * 把缓存条目映射为树节点：code 作为节点 id，parentCode 作为 parent 链。
     *
     * @param microService 微服务缓存条目
     * @return 树节点
     * @author K
     * @since 1.0.0
     */
    private fun toTreeNode(microService: SysMicroServiceCacheEntry): IdAndNameTreeNode<String> =
        IdAndNameTreeNode(microService.code, microService.name, microService.parentCode)

    /**
     * 从 update 入参抽 code（这里 id 即 code）；要求实现 [IIdEntity] 且 id 是 String。
     *
     * @param any 更新入参
     * @return 微服务 code
     * @throws IllegalStateException 入参类型不被支持
     * @author K
     * @since 1.0.0
     */
    private fun requireMicroServiceCode(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新微服务时不支持的入参类型: ${any::class.qualifiedName}")
}
