package io.kudos.ms.sys.core.outline.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.cache.OutLineBySystemAndTenantCache
import io.kudos.ms.sys.core.outline.dao.SysOutLineDao
import io.kudos.ms.sys.core.outline.event.SysOutLineBatchDeleted
import io.kudos.ms.sys.core.outline.event.SysOutLineDeleted
import io.kudos.ms.sys.core.outline.event.SysOutLineInserted
import io.kudos.ms.sys.core.outline.event.SysOutLineUpdated
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import io.kudos.ms.sys.core.outline.service.iservice.ISysOutLineService
import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Outbound allowlist service.
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysOutLineService(
    dao: SysOutLineDao,
    private val outLineBySystemAndTenantCache: OutLineBySystemAndTenantCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysOutLine, SysOutLineDao>(dao), ISysOutLineService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun listActiveOutLines(systemCode: String, tenantId: String?): List<SysOutLineCacheEntry> =
        outLineBySystemAndTenantCache.listOutLines(systemCode, tenantId)

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val outLine = SysOutLine {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(outLine),
            log = log,
            successMessage = "Updated outbound allowlist id=$id active=$active.",
            failureMessage = "Failed to update outbound allowlist id=$id active=$active!",
        ) {
            eventPublisher.publishEvent(SysOutLineUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "Inserted outbound allowlist id=$id.") {
            eventPublisher.publishEvent(SysOutLineInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "outbound allowlist")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated outbound allowlist id=$id.",
            failureMessage = "Failed to update outbound allowlist id=$id!",
        ) {
            eventPublisher.publishEvent(SysOutLineUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val po = dao.get(id)
        if (po == null) {
            log.warn("Outbound allowlist id=$id no longer exists when attempting delete!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted outbound allowlist id=$id.",
            failureMessage = "Failed to delete outbound allowlist id=$id!",
        ) {
            eventPublisher.publishEvent(
                SysOutLineDeleted(id = id, systemCode = po.systemCode, tenantId = po.tenantId)
            )
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val pos = dao.inSearchById(ids)
        val dimensions = pos.map { it.systemCode to it.tenantId }.toSet()
        val count = super.batchDelete(ids)
        log.debug("Batch delete outbound allowlist: expected ${ids.size}, actually deleted $count.")
        if (count > 0) {
            eventPublisher.publishEvent(SysOutLineBatchDeleted(ids = ids, dimensions = dimensions))
        }
        return count
    }

}
