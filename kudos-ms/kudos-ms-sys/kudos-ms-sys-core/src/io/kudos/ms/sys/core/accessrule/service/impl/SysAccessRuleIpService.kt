package io.kudos.ms.sys.core.accessrule.service.impl

import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.sys.common.accessrule.enums.AccessRuleTypeEnum
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
import java.time.LocalDateTime


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
     * Check whether the given integer IP is **allowed to access** under the current system and tenant dimensions.
     *
     * Decision semantics (delegated to [decideIpAccess]; blacklist and whitelist rules are
     * distinguished by [SysAccessRuleIpCacheEntry.accessRuleTypeDictCode]):
     * - hit on a non-expired blacklist rule = deny;
     * - when whitelist rules exist for the dimension, allow only if a non-expired whitelist rule is hit;
     * - no effective rule configured = allow by default.
     */
    @Transactional(readOnly = true)
    override fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean =
        decideIpAccess(ip, getIpsBySystemAndTenant(systemCode, tenantId), LocalDateTime.now())

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

/**
 * Pure decision function for IP access: returns whether [ip] is **allowed** to access given the
 * effective rule entries of one `(systemCode, tenantId)` dimension.
 *
 * Rules whose [SysAccessRuleIpCacheEntry.expirationTime] is before [now] are ignored. For the rest:
 * 1. A hit on a blacklist-typed entry ([AccessRuleTypeEnum.BLACKLIST]) denies access immediately.
 * 2. If whitelist-typed entries exist ([AccessRuleTypeEnum.WHITELIST] or
 *    [AccessRuleTypeEnum.WHITELIST_BLACKLIST] -- the latter cannot be told apart per entry yet,
 *    so it is treated conservatively as a whitelist member), access is allowed only when one of
 *    them is hit; otherwise access is denied (whitelist mode = deny by default).
 * 3. When no effective entry exists, or only [AccessRuleTypeEnum.UNLIMITED]-typed entries exist,
 *    access is allowed by default (no restriction configured).
 *
 * Entries with a `null` rule type (legacy cache data serialized before the type field existed)
 * never deny access: they neither act as blacklist nor add a whitelist requirement.
 *
 * Exposed as `internal` purely for unit testing.
 *
 * @param ip BigDecimal representation of the IP to check
 * @param rules cache entries of the dimension being checked
 * @param now reference time for expiration filtering
 * @return `true` if access is allowed, `false` if denied
 * @author K
 * @since 1.0.0
 */
internal fun decideIpAccess(
    ip: BigDecimal,
    rules: List<SysAccessRuleIpCacheEntry>,
    now: LocalDateTime,
): Boolean {
    val effective = rules.filter { it.expirationTime?.isBefore(now) != true }
    if (effective.isEmpty()) return true

    fun hit(rule: SysAccessRuleIpCacheEntry) = ip in rule.ipStart..rule.ipEnd

    val blacklistHit = effective.any {
        it.accessRuleTypeDictCode == AccessRuleTypeEnum.BLACKLIST.code && hit(it)
    }
    if (blacklistHit) return false

    val whitelistRules = effective.filter {
        it.accessRuleTypeDictCode == AccessRuleTypeEnum.WHITELIST.code ||
                it.accessRuleTypeDictCode == AccessRuleTypeEnum.WHITELIST_BLACKLIST.code
    }
    return whitelistRules.isEmpty() || whitelistRules.any { hit(it) }
}
