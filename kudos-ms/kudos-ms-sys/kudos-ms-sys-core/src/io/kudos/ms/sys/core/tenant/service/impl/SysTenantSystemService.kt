package io.kudos.ms.sys.core.tenant.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.tenant.cache.SysTenantSystemHashCache
import io.kudos.ms.sys.core.tenant.dao.SysTenantSystemDao
import io.kudos.ms.sys.core.tenant.event.SysTenantSystemBound
import io.kudos.ms.sys.core.tenant.event.SysTenantSystemSystemsChanged
import io.kudos.ms.sys.core.tenant.event.SysTenantSystemTenantsChanged
import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantSystemService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Tenant-system relation service.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysTenantSystemService(
    dao: SysTenantSystemDao,
    private val sysTenantSystemHashCache: SysTenantSystemHashCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysTenantSystem, SysTenantSystemDao>(dao), ISysTenantSystemService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun searchSystemCodesByTenantId(tenantId: String): Set<String> =
        dao.searchSystemCodesByTenantId(tenantId)

    @Transactional(readOnly = true)
    override fun searchTenantIdsBySystemCode(systemCode: String): Set<String> =
        sysTenantSystemHashCache.getTenantIdsBySubSystemCode(systemCode).toSet()

    @Transactional(readOnly = true)
    override fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> =
        dao.groupingSystemCodesByTenantIds(tenantIds)

    @Transactional(readOnly = true)
    override fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>?): Map<String, List<String>> =
        dao.groupingTenantIdsBySystemCodes(systemCodes)

    /**
     * Batch bind tenant-system relations.
     *
     * @param tenantId tenant id
     * @param systemCodes system code collection
     * @return number of successfully bound relations
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun batchBind(tenantId: String, systemCodes: Collection<String>): Int {
        if (systemCodes.isEmpty()) return 0

        // One SELECT for existing relations, then a single batchInsert for the diff — collapses the original N+1 into 2 statements.
        val existing = dao.searchSystemCodesByTenantId(tenantId)
        val insertedSystemCodes = (systemCodes.toSet() - existing)
        if (insertedSystemCodes.isEmpty()) {
            log.debug("Batch bind tenant=$tenantId to ${systemCodes.size} systems: all already exist, nothing to insert.")
            return 0
        }
        val relations = insertedSystemCodes.map {
            SysTenantSystem {
                this.tenantId = tenantId
                this.systemCode = it
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch bind tenant=$tenantId to ${systemCodes.size} systems: successfully inserted ${insertedSystemCodes.size}.")
        // Affects system-dim caches; tenant-dim cache will reload on demand.
        eventPublisher.publishEvent(SysTenantSystemSystemsChanged(systemCodes = insertedSystemCodes))
        return insertedSystemCodes.size
    }

    /**
     * Unbind a tenant-system relation.
     *
     * @param tenantId tenant id
     * @param systemCode system code
     * @return whether the unbind succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun unbind(tenantId: String, systemCode: String): Boolean {
        val count = dao.deleteByTenantIdAndSystemCode(tenantId, systemCode)
        val success = count > 0
        if (success) {
            log.debug("Unbound tenant=$tenantId from system=$systemCode.")
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = listOf(tenantId)))
        } else {
            log.warn("Failed to unbind tenant=$tenantId from system=$systemCode: relation does not exist.")
        }
        return success
    }

    /**
     * Check whether the relation exists.
     *
     * @param tenantId tenant id
     * @param systemCode system code
     * @return whether the relation exists
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional(readOnly = true)
    override fun exists(tenantId: String, systemCode: String): Boolean = dao.exists(tenantId, systemCode)

    @Transactional
    override fun deleteByTenantId(tenantId: String): Int {
        val systemCodes = searchSystemCodesByTenantId(tenantId)
        val count = dao.batchDeleteByTenantIds(listOf(tenantId))
        if (count > 0 && systemCodes.isNotEmpty()) {
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = listOf(tenantId)))
        }
        return count
    }

    @Transactional
    override fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int {
        if (tenantIds.isEmpty()) return 0
        val tenantAndSystemCodes = groupingSystemCodesByTenantIds(tenantIds)
        val systemCodes = tenantAndSystemCodes.values.flatten().toSet()
        val count = dao.batchDeleteByTenantIds(tenantIds)
        if (count > 0 && systemCodes.isNotEmpty()) {
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = tenantIds))
        }
        return count
    }

    /**
     * Insert a tenant-system relation.
     *
     * @param any relation object
     * @return primary key
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("Inserted tenant-system relation id=$id.")
        dao.get(id)?.let { relation ->
            eventPublisher.publishEvent(
                SysTenantSystemBound(id = id, tenantId = relation.tenantId, systemCode = relation.systemCode)
            )
        }
        return id
    }

    /**
     * Delete a tenant-system relation.
     *
     * @param id primary key
     * @return whether the delete succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun deleteById(id: String): Boolean {
        val relation = dao.get(id) ?: run {
            log.warn("Tenant-system relation id=$id no longer exists when attempting delete!")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("Deleted tenant-system relation id=$id.")
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = listOf(relation.tenantId)))
        } else {
            log.error("Failed to delete tenant-system relation id=$id!")
        }
        return success
    }

    /**
     * Batch delete tenant-system relations.
     *
     * @param ids primary key collection
     * @return number of relations deleted
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        @Suppress("UNCHECKED_CAST")
        val relations = dao.inSearchById(ids)
        val affectedTenantIds = relations.map { it.tenantId }.toSet()
        val count = super.batchDelete(ids)
        log.debug("Batch delete tenant-system relations: expected ${ids.size}, actually deleted $count.")
        if (count > 0 && affectedTenantIds.isNotEmpty()) {
            eventPublisher.publishEvent(SysTenantSystemTenantsChanged(tenantIds = affectedTenantIds))
        }
        return count
    }
}
