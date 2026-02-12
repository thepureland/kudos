package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.accessrule.SysAccessRuleRecord
import io.kudos.ms.sys.common.vo.accessrule.SysAccessRuleSearchPayload
import io.kudos.ms.sys.core.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.model.po.SysAccessRule
import io.kudos.ms.sys.core.service.iservice.ISysAccessRuleService
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

    override fun getAccessRuleByTenantAndSystem(tenantId: String?, systemCode: String): SysAccessRuleRecord? {
        val searchPayload = SysAccessRuleSearchPayload().apply {
            this.tenantId = tenantId
            this.systemCode = systemCode
        }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload, SysAccessRuleRecord::class)
        return records.firstOrNull()
    }

    override fun getAccessRulesByTenantId(tenantId: String): List<SysAccessRuleRecord> {
        val searchPayload = SysAccessRuleSearchPayload().apply {
            this.tenantId = tenantId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload, SysAccessRuleRecord::class)
    }

    override fun getAccessRulesBySystemCode(systemCode: String): List<SysAccessRuleRecord> {
        val searchPayload = SysAccessRuleSearchPayload().apply {
            this.systemCode = systemCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload, SysAccessRuleRecord::class)
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
