package io.kudos.ms.sys.core.accessrule.api

import io.kudos.ms.sys.common.accessrule.api.ISysAccessRuleIpApi
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleIpService
import org.springframework.stereotype.Service
import java.math.BigDecimal


/**
 * ip访问规则 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysAccessRuleIpApi(
    private val sysAccessRuleIpService: ISysAccessRuleIpService,
) : ISysAccessRuleIpApi {

    override fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow> = sysAccessRuleIpService.getIpsByRuleId(ruleId)

    override fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry> =
        sysAccessRuleIpService.getIpsBySystemAndTenant(systemCode, tenantId)

    override fun checkIpAccess(ip: BigDecimal, systemCode: String, tenantId: String?): Boolean =
        sysAccessRuleIpService.checkIpAccess(ip, systemCode, tenantId)


    override fun deleteByRuleId(ruleId: String): Int = sysAccessRuleIpService.deleteByRuleId(ruleId)
}
