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
 * Microservice service implementation.
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
            successMessage = "Updated active=${active} for microservice with code ${code}.",
            failureMessage = "Failed to update active=${active} for microservice with code ${code}!",
        ) {
            eventPublisher.publishEvent(SysMicroServiceUpdated(id = code))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        completeCrudInsert(log, "Inserted microservice with code ${code}.") {
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
            successMessage = "Updated microservice with code ${code}.",
            failureMessage = "Failed to update microservice with code ${code}!",
        ) {
            eventPublisher.publishEvent(SysMicroServiceUpdated(id = code))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val microService = dao.get(id)
        if (microService == null) {
            log.warn("Attempt to delete microservice with code ${id}, but it no longer exists!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Successfully deleted microservice with code ${id}!",
            failureMessage = "Failed to delete microservice with code ${id}!",
        ) {
            eventPublisher.publishEvent(SysMicroServiceDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("Batch delete microservices: expected ${ids.size}, actually deleted ${count}.")
        if (count > 0) {
            eventPublisher.publishEvent(SysMicroServiceBatchDeleted(ids = ids))
        }
        return count
    }

    /**
     * Map a cache entry to a tree node: code is the node id, parentCode is the parent link.
     *
     * @param microService Microservice cache entry
     * @return Tree node
     * @author K
     * @since 1.0.0
     */
    private fun toTreeNode(microService: SysMicroServiceCacheEntry): IdAndNameTreeNode<String> =
        IdAndNameTreeNode(microService.code, microService.name, microService.parentCode)

    /**
     * Extract code from the update payload (id == code here); requires the payload to implement [IIdEntity] with a String id.
     *
     * @param any Update payload
     * @return Microservice code
     * @throws IllegalStateException Payload type is not supported
     * @author K
     * @since 1.0.0
     */
    private fun requireMicroServiceCode(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("Unsupported payload type when updating microservice: ${any::class.qualifiedName}")
}
