package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.vo.accessruleip.request.SysAccessRuleIpBatchItem
import io.kudos.ms.sys.common.vo.accessruleip.request.SysAccessRuleIpQuery
import io.kudos.ms.sys.common.vo.accessruleip.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.cache.AccessRuleIpsBySubSysAndTenantIdCache
import io.kudos.ms.sys.core.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.dao.SysAccessRuleIpDao
import io.kudos.ms.sys.core.model.po.SysAccessRuleIp
import io.kudos.ms.sys.core.service.iservice.ISysAccessRuleIpService
import org.springframework.beans.factory.annotation.Autowired
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
    dao: SysAccessRuleIpDao
) : BaseCrudService<String, SysAccessRuleIp, SysAccessRuleIpDao>(dao), ISysAccessRuleIpService {


    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var accessRuleIpsBySubSysAndTenantIdCache: AccessRuleIpsBySubSysAndTenantIdCache

    @Autowired
    private lateinit var sysAccessRuleDao: SysAccessRuleDao

    override fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow> {
        val searchPayload = SysAccessRuleIpQuery(
            parentRuleId = ruleId
        )
        return dao.pagingSearch(searchPayload)
    }

    override fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry> {
        return accessRuleIpsBySubSysAndTenantIdCache.getAccessRuleIps(systemCode, tenantId)
    }

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
        var count = 0
        ips.forEach { form ->
            if (form.id.isNullOrBlank()) {
                val ipStart = requireNotNull(form.ipStart) { "新增IP规则时，ipStart不能为空。" }
                val ipEnd = requireNotNull(form.ipEnd) { "新增IP规则时，ipEnd不能为空。" }
                val ipRule = SysAccessRuleIp {
                    this.parentRuleId = ruleId
                    this.ipStart = ipStart
                    this.ipEnd = ipEnd
                    this.ipTypeDictCode = form.ipType?.toString() ?: "0"
                    this.expirationTime = form.expirationDate
                    this.active = form.active ?: true
                }
                val id = dao.insert(ipRule)
                accessRuleIpsBySubSysAndTenantIdCache.syncOnInsert(ipRule, id)
                count++
            } else {
                val ipStart = requireNotNull(form.ipStart) { "更新IP规则时，ipStart不能为空。" }
                val ipEnd = requireNotNull(form.ipEnd) { "更新IP规则时，ipEnd不能为空。" }
                val ipRule = SysAccessRuleIp {
                    this.id = form.id!!
                    this.parentRuleId = ruleId
                    this.ipStart = ipStart
                    this.ipEnd = ipEnd
                    this.ipTypeDictCode = form.ipType?.toString() ?: "0"
                    this.expirationTime = form.expirationDate
                    this.active = form.active ?: true
                }
                if (dao.update(ipRule)) {
                    accessRuleIpsBySubSysAndTenantIdCache.syncOnUpdate(ipRule, form.id!!)
                    count++
                }
            }
        }
        log.debug("批量保存或更新IP规则，期望处理${ips.size}条，实际处理${count}条。")
        // 同步父规则缓存
        val accessRule = sysAccessRuleDao.get(ruleId)
        if (accessRule != null) {
            accessRuleIpsBySubSysAndTenantIdCache.syncOnUpdate(accessRule, ruleId)
        }
        return count
    }

    @Transactional
    override fun deleteByRuleId(ruleId: String): Int {
        val count = dao.deleteByParentRuleId(ruleId)
        log.debug("删除规则${ruleId}的所有IP，共删除${count}条。")
        // 同步缓存
        val accessRule = sysAccessRuleDao.get(ruleId)
        if (accessRule != null) {
            accessRuleIpsBySubSysAndTenantIdCache.syncOnUpdate(accessRule, ruleId)
        }
        return count
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的IP访问规则。")
        val ipRule = dao.get(id)
        if (ipRule != null) {
            val accessRule = sysAccessRuleDao.get(ipRule.parentRuleId)
            if (accessRule != null) {
                accessRuleIpsBySubSysAndTenantIdCache.syncOnInsert(accessRule, id)
            }
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysAccessRuleIp::id.name) as String
        if (success) {
            log.debug("更新id为${id}的IP访问规则。")
            val ipRule = dao.get(id)
            if (ipRule != null) {
                val accessRule = sysAccessRuleDao.get(ipRule.parentRuleId)
                if (accessRule != null) {
                    accessRuleIpsBySubSysAndTenantIdCache.syncOnUpdate(accessRule, id)
                }
            }
        } else {
            log.error("更新id为${id}的IP访问规则失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val ipRule = dao.get(id)
        val accessRule = ipRule?.let { sysAccessRuleDao.get(it.parentRuleId) }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的IP访问规则。")
            if (accessRule != null) {
                accessRuleIpsBySubSysAndTenantIdCache.syncOnDeleteBySystemAndTenant(
                    accessRule.systemCode,
                    accessRule.tenantId
                )
            } else {
                accessRuleIpsBySubSysAndTenantIdCache.syncOnDelete(id)
            }
        } else {
            log.error("删除id为${id}的IP访问规则失败！")
        }
        return success
    }


}
