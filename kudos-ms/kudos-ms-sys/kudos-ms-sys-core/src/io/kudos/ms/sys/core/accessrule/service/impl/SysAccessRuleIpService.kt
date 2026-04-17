package io.kudos.ms.sys.core.accessrule.service.impl

import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.sys.common.accessrule.enums.SysAccessRuleErrorCodeEnum
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleFormCreate
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpFormCreate
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpQuery
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.cache.AccessRuleIpsBySubSysAndTenantIdCache
import io.kudos.ms.sys.core.accessrule.cache.SysAccessRuleHashCache
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleIpDao
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRuleIp
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleIpService
import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal


/**
 * IP 访问规则（`sys_access_rule_ip`）的增删改查与缓存同步；并与父表 `sys_access_rule` 联动刷新
 * [AccessRuleIpsBySubSysAndTenantIdCache]。
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
) : BaseCrudService<String, SysAccessRuleIp, SysAccessRuleIpDao>(dao), ISysAccessRuleIpService {

    private val log = LogFactory.getLog(this::class)

    @Resource
    private lateinit var sysAccessRuleHashCache: SysAccessRuleHashCache

    override fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow> =
        dao.pagingSearch(SysAccessRuleIpQuery(parentRuleId = ruleId))

    override fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry> =
        accessRuleIpsBySubSysAndTenantIdCache.getAccessRuleIps(systemCode, tenantId)

    /**
     * 判断给定整型 IP 是否落在当前系统与租户维度下任一未过期且区间包含该值的规则内。
     */
    override fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean {
        val ipRules = getIpsBySystemAndTenant(systemCode, tenantId)
        val now = java.time.LocalDateTime.now()
        
        return ipRules.any { rule ->
            // 检查是否过期
            val expirationTime = rule.expirationTime
            if (expirationTime != null && expirationTime.isBefore(now)) {
                return@any false
            }
            // 检查IP是否在范围内
            ip >= rule.ipStart && ip <= rule.ipEnd
        }
    }

    @Transactional
    override fun deleteByRuleId(ruleId: String): Int {
        val count = dao.deleteByParentRuleId(ruleId)
        log.debug("删除规则${ruleId}的所有IP，共删除${count}条。")
        syncParentRuleCache(ruleId)
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
            findParentAccessRuleByIpRuleId(id)?.let {
                accessRuleIpsBySubSysAndTenantIdCache.syncOnInsert(it, id)
            }
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
            findParentAccessRuleByIpRuleId(id)?.let {
                accessRuleIpsBySubSysAndTenantIdCache.syncOnUpdate(it, id)
            }
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
            findParentAccessRuleByIpRuleId(id)?.let {
                accessRuleIpsBySubSysAndTenantIdCache.syncOnUpdate(it, id)
            }
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val accessRule = findParentAccessRuleByIpRuleId(id)
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的IP访问规则。",
            failureMessage = "删除id为${id}的IP访问规则失败！",
        ) {
            if (accessRule != null) {
                accessRuleIpsBySubSysAndTenantIdCache.syncOnDeleteBySystemAndTenant(
                    accessRule.systemCode,
                    accessRule.tenantId
                )
            } else {
                accessRuleIpsBySubSysAndTenantIdCache.syncOnDelete(id)
            }
        }
    }

    private fun requireIpRuleId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新IP访问规则时不支持的入参类型: ${any::class.qualifiedName}")

    /** 根据 IP 规则主键加载其父访问规则（用于缓存维度）。 */
    private fun findParentAccessRuleByIpRuleId(id: String) =
        dao.get(id)?.let { sysAccessRuleDao.get(it.parentRuleId) }

    /** 在批量变更某父规则下 IP 后，按父规则刷新对应维度的 IP 规则缓存。 */
    private fun syncParentRuleCache(ruleId: String) {
        val accessRule = sysAccessRuleDao.get(ruleId) ?: return
        accessRuleIpsBySubSysAndTenantIdCache.syncOnUpdate(accessRule, ruleId)
    }

    /**
     * 批量删除 IP 规则；删除成功后对涉及父规则对应的「系统编码 + 租户」缓存维度分别执行 evict 与回填。
     */
    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        if (ids.isEmpty()) return 0
        val dimensionKeys = ids.mapNotNull { id ->
            dao.get(id)?.let { ip ->
                sysAccessRuleDao.get(ip.parentRuleId)?.let { r -> r.systemCode to r.tenantId }
            }
        }.distinct()
        val count = super.batchDelete(ids)
        if (count > 0) {
            dimensionKeys.forEach { (systemCode, tenantId) ->
                accessRuleIpsBySubSysAndTenantIdCache.syncOnDeleteBySystemAndTenant(systemCode, tenantId)
            }
        }
        return count
    }
}
