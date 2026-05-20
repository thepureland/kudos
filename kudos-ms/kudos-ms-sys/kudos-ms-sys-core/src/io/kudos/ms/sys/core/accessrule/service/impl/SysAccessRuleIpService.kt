package io.kudos.ms.sys.core.accessrule.service.impl

import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
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
 * IP 访问规则（`sys_access_rule_ip`）的增删改查；写后发布
 * [SysAccessRuleIpInserted] / [SysAccessRuleIpUpdated] / [SysAccessRuleIpDeleted] / [SysAccessRuleIpBatchDeleted]
 * 领域事件，由缓存通过 `@TransactionalEventListener(AFTER_COMMIT)` 订阅刷新。
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
     * 判断给定整型 IP 是否落在当前系统与租户维度下任一未过期且区间包含该值的规则内。
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
        log.debug("删除规则${ruleId}的所有IP，共删除${count}条。")
        if (parent != null && count > 0) {
            eventPublisher.publishEvent(
                SysAccessRuleIpBatchDeleted(
                    ids = emptyList(), // ids 对此场景非必要，订阅方按 dimensions 重建
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
        completeCrudInsert(log, "新增id为${id}的IP访问规则。") {
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
        val id = requireIpRuleId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的IP访问规则。",
            failureMessage = "更新id为${id}的IP访问规则失败！",
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
            log.error("更新id为${id}的IP访问规则的启用状态时，记录不存在！")
            return false
        }
        val patch = SysAccessRuleIp {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(patch),
            log = log,
            successMessage = "更新id为${id}的IP访问规则的启用状态为${active}。",
            failureMessage = "更新id为${id}的IP访问规则的启用状态为${active}失败！",
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
            successMessage = "删除id为${id}的IP访问规则。",
            failureMessage = "删除id为${id}的IP访问规则失败！",
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
                log.warn("删除id为${id}的IP访问规则后无法定位父规则，跳过事件发布；缓存仅能等下次 reloadAll 修正。")
            }
        }
    }

    /**
     * 从 update 入参抽 id；要求实现 [IIdEntity] 且 id 是 String。
     *
     * @param any 更新入参
     * @return IP 访问规则 id
     * @throws IllegalStateException 入参类型不被支持
     * @author K
     * @since 1.0.0
     */
    private fun requireIpRuleId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新IP访问规则时不支持的入参类型: ${any::class.qualifiedName}")

    /**
     * 批量删除 IP 规则；删除成功后发布 [SysAccessRuleIpBatchDeleted]。
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
