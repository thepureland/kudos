package io.kudos.ms.sys.core.accessrule.api
import io.kudos.ms.sys.common.accessrule.api.ISysAccessRuleApi
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleService
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
