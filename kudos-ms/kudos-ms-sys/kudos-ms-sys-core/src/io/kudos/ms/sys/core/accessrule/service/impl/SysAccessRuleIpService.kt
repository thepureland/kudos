package io.kudos.ms.sys.core.accessrule.service.impl

import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.sys.common.accessrule.enums.SysAccessRuleErrorCodeEnum
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpFormCreate
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpQuery
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.cache.AccessRuleIpsBySubSysAndTenantIdCache
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleIpDao
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpBatchDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpInserted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpUpdated
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleIpService
import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate
import jakarta.annotation.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal


/**
 * CRUD for IP access rules (`sys_access_rule_ip`); after writes, publishes
 * [SysAccessRuleIpInserted] / [SysAccessRuleIpUpdated] / [SysAccessRuleIpDeleted] / [SysAccessRuleIpBatchDeleted]
 * domain events, subscribed by the cache via `@TransactionalEventListener(AFTER_COMMIT)` to refresh.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysAccessRuleIpService(
    dao: SysAccessRuleIpDao,
    private val accessRuleIpsBySubSysAndTenantIdCache: AccessRuleIpsBySubSysAndTenantIdCache,
    private val sysAccessRuleDao: SysAccessRuleDao,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysAccessRuleIp, SysAccessRuleIpDao>(dao), ISysAccessRuleIpService {

    private val log = LogFactory.getLog(this::class)

    @Resource
    private lateinit var sysAccessRuleHashCache: io.kudos.ms.sys.core.accessrule.cache.SysAccessRuleHashCache

    @Transactional(readOnly = true)
    override fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow> =
        dao.pagingSearch(SysAccessRuleIpQuery(parentRuleId = ruleId))

    @Transactional(readOnly = true)
    override fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry> =
        accessRuleIpsBySubSysAndTenantIdCache.getAccessRuleIps(systemCode, tenantId)

    /**
     * Check whether the given integer IP falls within any non-expired rule whose range
     * contains the value under the current system and tenant dimensions.
     */
    @Transactional(readOnly = true)
    override fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean {
        val now = java.time.LocalDateTime.now()
        return getIpsBySystemAndTenant(systemCode, tenantId).any { rule ->
            rule.expirationTime?.isBefore(now) != true && ip in rule.ipStart..rule.ipEnd
        }
    }

    @Transactional
    override fun deleteByRuleId(ruleId: String): Int {
        val parent = sysAccessRuleDao.get(ruleId)
        val count = dao.deleteByParentRuleId(ruleId)
        log.debug("Deleted all IPs of rule ${ruleId}; ${count} records deleted in total.")
        if (parent != null && count > 0) {
            eventPublisher.publishEvent(
                SysAccessRuleIpBatchDeleted(
                    ids = emptyList(), // ids are not needed for this scenario; subscribers rebuild by dimensions
                    dimensions = listOf(parent.systemCode to parent.tenantId as String?),
                )
            )
        }
        return count
    }

    @Transactional
    override fun insert(any: Any): String {
        val ruleIp = if (any is SysAccessRuleIpFormCreate) {
            val cacheEntry = sysAccessRuleHashCache.getAccessRuleBySystemCodeAndTenantId(any.systemCode, any.tenantId)
                ?: throw ServiceException(SysAccessRuleErrorCodeEnum.PARENT_ACCESS_RULE_NOT_FOUND)
            SysAccessRuleIp.of(any, cacheEntry.id)
        } else any
        val id = super.insert(ruleIp)
        completeCrudInsert(log, "Inserted IP access rule with id ${id}.") {
            val ip = dao.get(id) ?: return@completeCrudInsert
            val parent = sysAccessRuleDao.get(ip.parentRuleId) ?: return@completeCrudInsert
            eventPublisher.publishEvent(
                SysAccessRuleIpInserted(
                    id = id,
                    parentSystemCode = parent.systemCode,
                    parentTenantId = parent.tenantId,
                    active = ip.active,
                )
            )
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "IP access rule")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated IP access rule with id ${id}.",
            failureMessage = "Failed to update IP access rule with id ${id}!",
        ) {
            val ip = dao.get(id) ?: return@completeCrudUpdate
            val parent = sysAccessRuleDao.get(ip.parentRuleId) ?: return@completeCrudUpdate
            eventPublisher.publishEvent(
                SysAccessRuleIpUpdated(
                    id = id,
                    parentSystemCode = parent.systemCode,
                    parentTenantId = parent.tenantId,
                    active = ip.active,
                )
            )
        }
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        if (dao.get(id) == null) {
            log.error("Record not found when updating active state of IP access rule with id ${id}!")
            return false
        }
        val patch = SysAccessRuleIp {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(patch),
            log = log,
            successMessage = "Updated active state of IP access rule with id ${id} to ${active}.",
            failureMessage = "Failed to update active state of IP access rule with id ${id} to ${active}!",
        ) {
            val ip = dao.get(id) ?: return@completeCrudUpdate
            val parent = sysAccessRuleDao.get(ip.parentRuleId) ?: return@completeCrudUpdate
            eventPublisher.publishEvent(
                SysAccessRuleIpUpdated(
                    id = id,
                    parentSystemCode = parent.systemCode,
                    parentTenantId = parent.tenantId,
                    active = ip.active,
                )
            )
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val accessRuleIp = dao.get(id)
        val parent = accessRuleIp?.let { sysAccessRuleDao.get(it.parentRuleId) }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted IP access rule with id ${id}.",
            failureMessage = "Failed to delete IP access rule with id ${id}!",
        ) {
            if (parent != null) {
                eventPublisher.publishEvent(
                    SysAccessRuleIpDeleted(
                        id = id,
                        parentSystemCode = parent.systemCode,
                        parentTenantId = parent.tenantId,
                    )
                )
            } else {
                log.warn("After deleting IP access rule with id ${id}, parent rule cannot be located; skipping event publish; cache can only be corrected by the next reloadAll.")
            }
        }
    }

    /**
     * Batch delete IP rules; publishes [SysAccessRuleIpBatchDeleted] on successful deletion.
     */
    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        if (ids.isEmpty()) return 0
        val dimensions = ids.mapNotNull { id ->
            dao.get(id)?.let { ip ->
                sysAccessRuleDao.get(ip.parentRuleId)?.let { r -> r.systemCode to r.tenantId as String? }
            }
        }.distinct()
        val count = super.batchDelete(ids)
        if (count > 0) {
            eventPublisher.publishEvent(SysAccessRuleIpBatchDeleted(ids = ids, dimensions = dimensions))
        }
        return count
    }
}
