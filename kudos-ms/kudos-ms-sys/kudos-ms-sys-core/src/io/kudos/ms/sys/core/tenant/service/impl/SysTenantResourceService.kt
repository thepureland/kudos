package io.kudos.ms.sys.core.tenant.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.tenant.dao.SysTenantResourceDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenantResource
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantResourceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Tenant-resource relation service.
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysTenantResourceService(
    dao: SysTenantResourceDao
) : BaseCrudService<String, SysTenantResource, SysTenantResourceDao>(dao), ISysTenantResourceService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getResourceIdsByTenantId(tenantId: String): Set<String> = dao.searchResourceIdsByTenantId(tenantId)

    @Transactional(readOnly = true)
    override fun getTenantIdsByResourceId(resourceId: String): Set<String> = dao.searchTenantIdsByResourceId(resourceId)

    @Transactional
    override fun batchBind(tenantId: String, resourceIds: Collection<String>): Int {
        if (resourceIds.isEmpty()) return 0

        // One SELECT for the full set of existing relations, then a single batchInsert for the diff;
        // collapses the original N+1 (1 exists + 1 insert per id) into 2 statements.
        val existing = dao.searchResourceIdsByTenantId(tenantId)
        val newResourceIds = resourceIds.toSet() - existing
        if (newResourceIds.isEmpty()) {
            log.debug("Batch bind tenant=$tenantId to ${resourceIds.size} resources: all already exist, nothing to insert.")
            return 0
        }
        val relations = newResourceIds.map {
            SysTenantResource {
                this.tenantId = tenantId
                this.resourceId = it
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch bind tenant=$tenantId to ${resourceIds.size} resources: successfully inserted ${newResourceIds.size}.")
        return newResourceIds.size
    }

    @Transactional
    override fun unbind(tenantId: String, resourceId: String): Boolean {
        val count = dao.deleteByTenantIdAndResourceId(tenantId, resourceId)
        val success = count > 0
        if (success) {
            log.debug("Unbound tenant=$tenantId from resource=$resourceId.")
        } else {
            log.warn("Failed to unbind tenant=$tenantId from resource=$resourceId: relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(tenantId: String, resourceId: String): Boolean = dao.exists(tenantId, resourceId)
}
