package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysAccessRuleApi
import io.kudos.ms.sys.common.vo.accessrule.SysAccessRuleRow
import io.kudos.ms.sys.core.service.iservice.ISysAccessRuleService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 访问规则 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysAccessRuleApi : ISysAccessRuleApi {


    @Resource
    protected lateinit var sysAccessRuleService: ISysAccessRuleService

    override fun getAccessRuleByTenantAndSystem(
        systemCode: String,
        tenantId: String
    ): SysAccessRuleRow? {
        return sysAccessRuleService.getAccessRuleByTenantAndSystem(systemCode, tenantId)
    }


    override fun updateActive(id: String, active: Boolean): Boolean {
        return sysAccessRuleService.updateActive(id, active)
    }


}