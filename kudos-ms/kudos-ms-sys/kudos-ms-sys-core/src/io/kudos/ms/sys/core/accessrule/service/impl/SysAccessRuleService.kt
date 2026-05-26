package io.kudos.ms.sys.core.accessrule.service.impl

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.sys.common.accessrule.enums.SysAccessRuleErrorCodeEnum
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleFormCreate
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import io.kudos.ms.sys.core.accessrule.cache.SysAccessRuleHashCache
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleBatchDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleInserted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleUpdated
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleService
import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Persistence and query for access rules (`sys_access_rule`). After successful writes, publishes domain events
 * （[SysAccessRuleInserted] / [SysAccessRuleUpdated] / [SysAccessRuleDeleted] / [SysAccessRuleBatchDeleted]），
 * which caches subscribe to via `@TransactionalEventListener(AFTER_COMMIT)` to refresh themselves.
 *
 * This layer no longer holds any cache handles directly (except [SysAccessRuleHashCache] needed for the pre-write uniqueness check).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysAccessRuleService(
    dao: SysAccessRuleDao,
    private val sysAccessRuleHashCache: SysAccessRuleHashCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysAccessRule, SysAccessRuleDao>(dao), ISysAccessRuleService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getAccessRuleByTenantAndSystem(
        systemCode: String,
        tenantId: String
    ): SysAccessRuleRow? = dao.searchAs<SysAccessRuleRow>(
        Criteria.and(
            SysAccessRule::tenantId eq tenantId,
            SysAccessRule::systemCode eq systemCode,
        )
    ).firstOrNull()

    @Transactional(readOnly = true)
    override fun getAccessRulesByTenantId(tenantId: String): List<SysAccessRuleRow> =
        dao.searchAs(Criteria.and(SysAccessRule::tenantId eq tenantId))

    @Transactional(readOnly = true)
    override fun getAccessRulesBySystemCode(systemCode: String): List<SysAccessRuleRow> =
        dao.searchAs(Criteria.and(SysAccessRule::systemCode eq systemCode))

    /**
     * Update only the enabled flag; on success publishes [SysAccessRuleUpdated] (dimension keys are the original ones; subscribers refresh the IP cache accordingly).
     */
    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val existing = dao.get(id) ?: run {
            log.error("Cannot update active state of access rule id=$id: record does not exist!")
            return false
        }
        val accessRule = SysAccessRule {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(accessRule),
            log = log,
            successMessage = "Updated access rule id=$id active=$active.",
            failureMessage = "Failed to update access rule id=$id active=$active!",
        ) {
            eventPublisher.publishEvent(
                SysAccessRuleUpdated(
                    id = id,
                    systemCode = existing.systemCode,
                    tenantId = existing.tenantId,
                    beforeSystemCode = existing.systemCode,
                    beforeTenantId = existing.tenantId,
                )
            )
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        if (any !is SysAccessRuleFormCreate) {
            throw ServiceException(CommonErrorCodeEnum.BAD_REQUEST)
        }

        if (sysAccessRuleHashCache.getAccessRuleBySystemCodeAndTenantId(any.systemCode, any.tenantId) != null) {
            throw ServiceException(SysAccessRuleErrorCodeEnum.ACCESS_RULE_ALREADY_EXISTS)
        }

        val id = super.insert(any)
        completeCrudInsert(log, "Inserted access rule id=$id.") {
            val rule = dao.get(id) ?: return@completeCrudInsert
            eventPublisher.publishEvent(
                SysAccessRuleInserted(
                    id = id,
                    systemCode = rule.systemCode,
                    tenantId = rule.tenantId,
                )
            )
        }
        return id
    }

    /**
     * Update an access rule; on success publishes [SysAccessRuleUpdated] and subscribers handle old-dimension refresh based on `dimensionChanged`.
     */
    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "access rule")
        val before = dao.get(id)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated access rule id=$id.",
            failureMessage = "Failed to update access rule id=$id!",
        ) {
            val after = dao.get(id) ?: return@completeCrudUpdate
            eventPublisher.publishEvent(
                SysAccessRuleUpdated(
                    id = id,
                    systemCode = after.systemCode,
                    tenantId = after.tenantId,
                    beforeSystemCode = before?.systemCode,
                    beforeTenantId = before?.tenantId,
                )
            )
        }
    }

    /**
     * Delete an access rule by primary key; on success publishes [SysAccessRuleDeleted].
     */
    @Transactional
    override fun deleteById(id: String): Boolean {
        val existing = dao.get(id) ?: run {
            log.warn("Access rule id=$id no longer exists when attempting delete!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted access rule id=$id.",
            failureMessage = "Failed to delete access rule id=$id!",
        ) {
            eventPublisher.publishEvent(
                SysAccessRuleDeleted(
                    id = id,
                    systemCode = existing.systemCode,
                    tenantId = existing.tenantId,
                )
            )
        }
    }

    /**
     * Batch delete access rules; if any rows are deleted, publishes [SysAccessRuleBatchDeleted] carrying all affected dimensions.
     */
    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        if (ids.isEmpty()) return 0
        val existing = ids.mapNotNull { dao.get(it) }
        if (existing.isEmpty()) return 0
        val dimensions = existing.map { it.systemCode to it.tenantId as String? }.distinct()
        val count = super.batchDelete(ids)
        if (count > 0) {
            eventPublisher.publishEvent(SysAccessRuleBatchDeleted(ids = ids, dimensions = dimensions))
        }
        return count
    }

}
