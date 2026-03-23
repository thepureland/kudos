package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.accessrule.response.SysAccessRuleRow
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
@Transactional
open class SysAccessRuleService(
    dao: SysAccessRuleDao
) : BaseCrudService<String, SysAccessRule, SysAccessRuleDao>(dao), ISysAccessRuleService {


    private val log = LogFactory.getLog(this)

    override fun getAccessRuleByTenantAndSystem(
        systemCode: String,
        tenantId: String
    ): SysAccessRuleRow? {
        val criteria = Criteria.and(
            SysAccessRule::tenantId eq tenantId,
            SysAccessRule::systemCode eq systemCode,
        )
        val records = dao.searchAs<SysAccessRuleRow>(criteria)
        return records.firstOrNull()
    }

    override fun getAccessRulesByTenantId(tenantId: String): List<SysAccessRuleRow> {
        val criteria = Criteria.and(SysAccessRule::tenantId eq tenantId)
        return dao.searchAs<SysAccessRuleRow>(criteria)
    }

    override fun getAccessRulesBySystemCode(systemCode: String): List<SysAccessRuleRow> {
        val criteria = Criteria.and(SysAccessRule::systemCode eq systemCode)
        return dao.searchAs<SysAccessRuleRow>(criteria)
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


}
