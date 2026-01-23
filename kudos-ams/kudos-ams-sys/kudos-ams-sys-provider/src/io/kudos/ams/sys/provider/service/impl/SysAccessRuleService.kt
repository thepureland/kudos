package io.kudos.ams.sys.provider.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ams.sys.common.vo.accessrule.SysAccessRuleRecord
import io.kudos.ams.sys.common.vo.accessrule.SysAccessRuleSearchPayload
import io.kudos.ams.sys.provider.dao.SysAccessRuleDao
import io.kudos.ams.sys.provider.model.po.SysAccessRule
import io.kudos.ams.sys.provider.service.iservice.ISysAccessRuleService
import io.kudos.base.logger.LogFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 访问规则业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysAccessRuleService : BaseCrudService<String, SysAccessRule, SysAccessRuleDao>(), ISysAccessRuleService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    override fun getAccessRuleByTenantAndSubSystem(tenantId: String?, subSystemCode: String?, portalCode: String): SysAccessRuleRecord? {
        val searchPayload = SysAccessRuleSearchPayload().apply {
            this.tenantId = tenantId
            this.subSystemCode = subSystemCode
            this.portalCode = portalCode
        }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload) as List<SysAccessRuleRecord>
        return records.firstOrNull()
    }

    override fun getAccessRulesByTenantId(tenantId: String): List<SysAccessRuleRecord> {
        val searchPayload = SysAccessRuleSearchPayload().apply {
            this.tenantId = tenantId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysAccessRuleRecord>
    }

    override fun getAccessRulesBySubSystemCode(subSystemCode: String): List<SysAccessRuleRecord> {
        val searchPayload = SysAccessRuleSearchPayload().apply {
            this.subSystemCode = subSystemCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysAccessRuleRecord>
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val accessRule = SysAccessRule {
            this.id = id
            this.active = active
        }
        val success = dao.update(accessRule)
        if (success) {
            log.debug("更新id为${id}的访问规则的启用状态为${active}。")
        } else {
            log.error("更新id为${id}的访问规则的启用状态为${active}失败！")
        }
        return success
    }

    //endregion your codes 2

}