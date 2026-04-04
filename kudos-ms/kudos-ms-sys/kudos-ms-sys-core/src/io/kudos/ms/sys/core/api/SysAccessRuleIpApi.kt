package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysAccessRuleIpApi
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.vo.accessruleip.request.SysAccessRuleIpBatchItem
import io.kudos.ms.sys.common.vo.accessruleip.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.service.iservice.ISysAccessRuleIpService
import org.springframework.stereotype.Service


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

    override fun checkIpAccess(ip: Long, systemCode: String, tenantId: String?): Boolean =
        sysAccessRuleIpService.checkIpAccess(ip, systemCode, tenantId)

    override fun batchSaveOrUpdate(ruleId: String, ips: List<SysAccessRuleIpBatchItem>): Int =
        sysAccessRuleIpService.batchSaveOrUpdate(ruleId, ips)

    override fun deleteByRuleId(ruleId: String): Int = sysAccessRuleIpService.deleteByRuleId(ruleId)
}
