package io.kudos.ms.sys.api.internal.controller.accessrule

import io.kudos.ms.sys.common.accessrule.api.ISysAccessRuleIpApi
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.api.SysAccessRuleIpApi
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal


/**
 * IP access rule internal RPC controller. Paths are inherited from method-level annotations on [ISysAccessRuleIpApi].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysAccessRuleIpInternalController(
    private val sysAccessRuleIpApi: SysAccessRuleIpApi,
) : ISysAccessRuleIpApi {

    override fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow> =
        sysAccessRuleIpApi.getIpsByRuleId(ruleId)

    override fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry> =
        sysAccessRuleIpApi.getIpsBySystemAndTenant(systemCode, tenantId)

    override fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean =
        sysAccessRuleIpApi.checkIpAccess(ip, systemCode, tenantId)

    override fun deleteByRuleId(ruleId: String): Int =
        sysAccessRuleIpApi.deleteByRuleId(ruleId)

}
