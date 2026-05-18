package io.kudos.ms.sys.api.internal.controller.accessrule

import io.kudos.ms.sys.common.accessrule.api.ISysAccessRuleApi
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import io.kudos.ms.sys.core.accessrule.api.SysAccessRuleApi
import org.springframework.web.bind.annotation.RestController


/**
 * 访问规则 内部 RPC 控制器。路径继承自 [ISysAccessRuleApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysAccessRuleInternalController(
    private val sysAccessRuleApi: SysAccessRuleApi,
) : ISysAccessRuleApi {

    override fun getAccessRuleByTenantAndSystem(systemCode: String, tenantId: String): SysAccessRuleRow? =
        sysAccessRuleApi.getAccessRuleByTenantAndSystem(systemCode, tenantId)

    override fun updateActive(id: String, active: Boolean): Boolean =
        sysAccessRuleApi.updateActive(id, active)

}
