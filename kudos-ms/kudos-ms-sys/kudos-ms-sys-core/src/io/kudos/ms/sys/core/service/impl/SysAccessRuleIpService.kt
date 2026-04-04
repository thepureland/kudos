package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.vo.accessruleip.request.SysAccessRuleIpBatchItem
import io.kudos.ms.sys.common.vo.accessruleip.request.SysAccessRuleIpQuery
import io.kudos.ms.sys.common.vo.accessruleip.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.cache.AccessRuleIpsBySubSysAndTenantIdCache
import io.kudos.ms.sys.core.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.dao.SysAccessRuleIpDao
import io.kudos.ms.sys.core.model.po.SysAccessRuleIp
import io.kudos.ms.sys.core.service.iservice.ISysAccessRuleIpService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * ip访问规则业务
 *
 * @author K
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

    override fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow> =
        dao.pagingSearch(SysAccessRuleIpQuery(parentRuleId = ruleId))

    override fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry> =
        accessRuleIpsBySubSysAndTenantIdCache.getAccessRuleIps(systemCode, tenantId)

    override fun checkIpAccess(ip: Long, systemCode: String, tenantId: String?): Boolean {
        val ipRules = getIpsBySystemAndTenant(systemCode, tenantId)
        val now = java.time.LocalDateTime.now()
        
        return ipRules.any { rule ->
            // 检查是否过期
            val expirationTime = rule.expirationTime
            if (expirationTime != null && expirationTime.isBefore(now)) {
                return@any false
            }
            // 检查IP是否在范围内
            ip >= (rule.ipStart ?: Long.MIN_VALUE) && ip <= (rule.ipEnd ?: Long.MAX_VALUE)
        }
    }

    @Transactional
    override fun batchSaveOrUpdate(ruleId: String, ips: List<SysAccessRuleIpBatchItem>): Int {
        val count = ips.count { form ->
            if (form.id.isNullOrBlank()) {
                insertIpRule(ruleId, form)
            } else {
                updateIpRule(ruleId, form)
            }
        }
        log.debug("批量保存或更新IP规则，期望处理${ips.size}条，实际处理${count}条。")
        syncParentRuleCache(ruleId)
        return count
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
        val id = super.insert(any)
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

    private fun insertIpRule(ruleId: String, form: SysAccessRuleIpBatchItem): Boolean {
        val ipRule = toIpRule(ruleId, form, creating = true)
        val id = dao.insert(ipRule)
        accessRuleIpsBySubSysAndTenantIdCache.syncOnInsert(ipRule, id)
        return true
    }

    private fun updateIpRule(ruleId: String, form: SysAccessRuleIpBatchItem): Boolean {
        val ipRule = toIpRule(ruleId, form, creating = false)
        val updated = dao.update(ipRule)
        if (updated) {
            accessRuleIpsBySubSysAndTenantIdCache.syncOnUpdate(ipRule, ipRule.id)
        }
        return updated
    }

    private fun toIpRule(ruleId: String, form: SysAccessRuleIpBatchItem, creating: Boolean): SysAccessRuleIp {
        val operation = if (creating) "新增" else "更新"
        val ipStart = requireNotNull(form.ipStart) { "${operation}IP规则时，ipStart不能为空。" }
        val ipEnd = requireNotNull(form.ipEnd) { "${operation}IP规则时，ipEnd不能为空。" }

        return SysAccessRuleIp {
            if (!creating) {
                this.id = requireNotNull(form.id) { "更新IP规则时，id不能为空。" }
            }
            this.parentRuleId = ruleId
            this.ipStart = ipStart
            this.ipEnd = ipEnd
            this.ipTypeDictCode = form.ipType?.toString() ?: "0"
            this.expirationTime = form.expirationDate
            this.active = form.active ?: true
        }
    }

    private fun findParentAccessRuleByIpRuleId(id: String) =
        dao.get(id)?.let { sysAccessRuleDao.get(it.parentRuleId) }

    private fun syncParentRuleCache(ruleId: String) {
        val accessRule = sysAccessRuleDao.get(ruleId) ?: return
        accessRuleIpsBySubSysAndTenantIdCache.syncOnUpdate(accessRule, ruleId)
    }
}
