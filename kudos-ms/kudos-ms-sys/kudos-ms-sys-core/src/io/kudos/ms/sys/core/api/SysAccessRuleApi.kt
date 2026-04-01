package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysAccessRuleApi
import io.kudos.ms.sys.common.vo.accessrule.response.SysAccessRuleRow
import io.kudos.ms.sys.core.service.iservice.ISysAccessRuleService
import org.springframework.stereotype.Service


/**
 * 访问规则 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysAccessRuleApi(
    private val sysAccessRuleService: ISysAccessRuleService,
) : ISysAccessRuleApi {

    override fun getAccessRuleByTenantAndSystem(
        systemCode: String,
        tenantId: String
    ): SysAccessRuleRow? = sysAccessRuleService.getAccessRuleByTenantAndSystem(systemCode, tenantId)

    override fun updateActive(id: String, active: Boolean): Boolean = sysAccessRuleService.updateActive(id, active)
}
