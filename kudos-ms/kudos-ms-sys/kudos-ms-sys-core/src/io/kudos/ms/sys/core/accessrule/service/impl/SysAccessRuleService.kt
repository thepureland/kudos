package io.kudos.ms.sys.core.accessrule.service.impl

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
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
 * 访问规则（`sys_access_rule`）的持久化与查询。写操作成功后发布领域事件
 * （[SysAccessRuleInserted] / [SysAccessRuleUpdated] / [SysAccessRuleDeleted] / [SysAccessRuleBatchDeleted]），
 * 由各缓存通过 `@TransactionalEventListener(AFTER_COMMIT)` 订阅并自行刷新。
 *
 * 这层不再直接持有任何 cache 句柄（除了写前唯一性预检需要的 [SysAccessRuleHashCache]）。
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
     * 仅更新启用标记；成功后发布 [SysAccessRuleUpdated]（维度键即原维度，订阅方据此刷新 IP 缓存）。
     */
    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val existing = dao.get(id) ?: run {
            log.error("更新id为${id}的访问规则的启用状态时，记录不存在！")
            return false
        }
        val accessRule = SysAccessRule {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(accessRule),
            log = log,
            successMessage = "更新id为${id}的访问规则的启用状态为${active}。",
            failureMessage = "更新id为${id}的访问规则的启用状态为${active}失败！",
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
        completeCrudInsert(log, "新增id为${id}的访问规则。") {
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
     * 更新访问规则；成功后发布 [SysAccessRuleUpdated]，订阅方据 `dimensionChanged` 自行处理旧维度刷新。
     */
    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireAccessRuleId(any)
        val before = dao.get(id)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的访问规则。",
            failureMessage = "更新id为${id}的访问规则失败！",
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
     * 按主键删除访问规则；成功后发布 [SysAccessRuleDeleted]。
     */
    @Transactional
    override fun deleteById(id: String): Boolean {
        val existing = dao.get(id) ?: run {
            log.warn("删除id为${id}的访问规则时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的访问规则。",
            failureMessage = "删除id为${id}的访问规则失败！",
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
     * 批量删除访问规则；若有成功删除行，则发布 [SysAccessRuleBatchDeleted] 携带所有受影响维度。
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

    /** 从更新入参中解析主键，非 [IIdEntity] 时抛出异常。 */
    private fun requireAccessRuleId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新访问规则时不支持的入参类型: ${any::class.qualifiedName}")
}
