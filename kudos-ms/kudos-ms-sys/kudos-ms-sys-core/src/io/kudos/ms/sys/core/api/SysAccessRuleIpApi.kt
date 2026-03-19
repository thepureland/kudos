package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysAccessRuleIpApi
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.vo.accessruleip.request.SysAccessRuleIpBatchItem
import io.kudos.ms.sys.common.vo.accessruleip.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.service.iservice.ISysAccessRuleIpService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * ip访问规则 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysAccessRuleIpApi : ISysAccessRuleIpApi {


    @Resource
    protected lateinit var sysAccessRuleIpService: ISysAccessRuleIpService

    override fun getIpsByRuleId(ruleId: String): List<SysAccessRuleIpRow> {
        return sysAccessRuleIpService.getIpsByRuleId(ruleId)
    }

    override fun getIpsBySystemAndTenant(systemCode: String, tenantId: String?): List<SysAccessRuleIpCacheEntry> {
        return sysAccessRuleIpService.getIpsBySystemAndTenant(systemCode, tenantId)
    }

    override fun checkIpAccess(ip: Long, systemCode: String, tenantId: String?): Boolean {
        return sysAccessRuleIpService.checkIpAccess(ip, systemCode, tenantId)
    }

    override fun batchSaveOrUpdate(ruleId: String, ips: List<SysAccessRuleIpBatchItem>): Int {
        return sysAccessRuleIpService.batchSaveOrUpdate(ruleId, ips)
    }

    override fun deleteByRuleId(ruleId: String): Int {
        return sysAccessRuleIpService.deleteByRuleId(ruleId)
    }


}
