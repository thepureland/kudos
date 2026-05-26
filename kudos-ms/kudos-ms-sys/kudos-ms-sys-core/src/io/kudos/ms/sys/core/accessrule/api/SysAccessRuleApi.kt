package io.kudos.ms.sys.core.accessrule.api

import io.kudos.ms.sys.common.accessrule.api.ISysAccessRuleApi
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * Local implementation of the access rule API.
 *
 * @author K
 * @since 1.0.0
 */
@Primary
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
