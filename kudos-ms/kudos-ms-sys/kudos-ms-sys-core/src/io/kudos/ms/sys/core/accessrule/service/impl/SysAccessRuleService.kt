package io.kudos.ms.sys.core.accessrule.service.impl
import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import io.kudos.ms.sys.core.accessrule.cache.AccessRuleIpsBySubSysAndTenantIdCache
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 访问规则（`sys_access_rule`）的持久化与查询；写操作成功后刷新
 * [AccessRuleIpsBySubSysAndTenantIdCache] 中按「系统编码 + 租户」维度的 IP 规则缓存，保证父规则变更与缓存一致。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysAccessRuleService(
    dao: SysAccessRuleDao,
    private val accessRuleIpsBySubSysAndTenantIdCache: AccessRuleIpsBySubSysAndTenantIdCache,
) : BaseCrudService<String, SysAccessRule, SysAccessRuleDao>(dao), ISysAccessRuleService {

    private val log = LogFactory.getLog(this::class)

    override fun getAccessRuleByTenantAndSystem(
        systemCode: String,
        tenantId: String
    ): SysAccessRuleRow? = dao.searchAs<SysAccessRuleRow>(
        Criteria.and(
            SysAccessRule::tenantId eq tenantId,
            SysAccessRule::systemCode eq systemCode,
        )
    ).firstOrNull()

    override fun getAccessRulesByTenantId(tenantId: String): List<SysAccessRuleRow> =
        dao.searchAs(Criteria.and(SysAccessRule::tenantId eq tenantId))

    override fun getAccessRulesBySystemCode(systemCode: String): List<SysAccessRuleRow> =
        dao.searchAs(Criteria.and(SysAccessRule::systemCode eq systemCode))

    /**
     * 仅更新启用标记；成功后按该规则原有系统编码、租户刷新 IP 规则缓存（父规则停用会影响缓存中 IP 列表是否生效）。
     */
    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val existing = dao.get(id)
        if (existing == null) {
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
            accessRuleIpsBySubSysAndTenantIdCache.syncOnDeleteBySystemAndTenant(
                existing.systemCode,
                existing.tenantId,
            )
        }
    }

    /**
     * 新增访问规则并在成功后按新行的系统编码、租户刷新 IP 规则缓存。
     */
    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的访问规则。") {
            val rule = dao.get(id) ?: return@completeCrudInsert
            accessRuleIpsBySubSysAndTenantIdCache.syncOnDeleteBySystemAndTenant(rule.systemCode, rule.tenantId)
        }
        return id
    }

    /**
     * 更新访问规则；成功后刷新当前维度缓存，若系统编码或租户发生变化则同时刷新旧维度。
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
            accessRuleIpsBySubSysAndTenantIdCache.syncOnDeleteBySystemAndTenant(after.systemCode, after.tenantId)
            if (before != null &&
                (before.systemCode != after.systemCode || before.tenantId != after.tenantId)
            ) {
                accessRuleIpsBySubSysAndTenantIdCache.syncOnDeleteBySystemAndTenant(
                    before.systemCode,
                    before.tenantId,
                )
            }
        }
    }

    /**
     * 按主键删除访问规则；成功后刷新对应系统编码、租户下的 IP 规则缓存。
     */
    @Transactional
    override fun deleteById(id: String): Boolean {
        val existing = dao.get(id)
        if (existing == null) {
            log.warn("删除id为${id}的访问规则时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的访问规则。",
            failureMessage = "删除id为${id}的访问规则失败！",
        ) {
            accessRuleIpsBySubSysAndTenantIdCache.syncOnDeleteBySystemAndTenant(
                existing.systemCode,
                existing.tenantId,
            )
        }
    }

    /**
     * 批量删除访问规则；若有成功删除行，则对所涉及各「系统编码 + 租户」维度分别刷新 IP 规则缓存。
     */
    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        if (ids.isEmpty()) return 0
        val existing = ids.mapNotNull { dao.get(it) }
        if (existing.isEmpty()) return 0
        val keys = existing.map { it.systemCode to it.tenantId }.distinct()
        val count = super.batchDelete(ids)
        if (count > 0) {
            keys.forEach { (systemCode, tenantId) ->
                accessRuleIpsBySubSysAndTenantIdCache.syncOnDeleteBySystemAndTenant(systemCode, tenantId)
            }
        }
        return count
    }

    /** 从更新入参中解析主键，非 [IIdEntity] 时抛出异常。 */
    private fun requireAccessRuleId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新访问规则时不支持的入参类型: ${any::class.qualifiedName}")
}
