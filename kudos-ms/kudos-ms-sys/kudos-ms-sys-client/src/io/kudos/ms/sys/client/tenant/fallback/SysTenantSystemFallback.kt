package io.kudos.ms.sys.client.tenant.fallback

import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.client.tenant.proxy.ISysTenantSystemProxy
import org.springframework.stereotype.Component


/**
 * 租户-子系统关系 Feign 容错降级实现。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysTenantSystemFallback : SysClientFallbackSupport("SysTenantSystemFallback"), ISysTenantSystemProxy {

    override fun searchSystemCodesByTenantId(tenantId: String): Set<String> {
        warnRead("searchSystemCodesByTenantId", tenantId)
        return emptySet()
    }

    override fun searchTenantIdsBySystemCode(systemCode: String): Set<String> {
        warnRead("searchTenantIdsBySystemCode", systemCode)
        return emptySet()
    }

    override fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> {
        warnRead("groupingSystemCodesByTenantIds", tenantIds)
        return emptyMap()
    }

    override fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>?): Map<String, List<String>> {
        warnRead("groupingTenantIdsBySystemCodes", systemCodes)
        return emptyMap()
    }

    override fun batchBind(tenantId: String, systemCodes: Collection<String>): Int {
        errorWrite("batchBind", tenantId, systemCodes)
        return 0
    }

    override fun unbind(tenantId: String, systemCode: String): Boolean {
        errorWrite("unbind", tenantId, systemCode)
        return false
    }

    override fun exists(tenantId: String, systemCode: String): Boolean {
        // 安全默认：远端不可达时按「未绑定」处理，避免错误授权
        warnRead("exists", tenantId, systemCode)
        return false
    }

    override fun deleteByTenantId(tenantId: String): Int {
        errorWrite("deleteByTenantId", tenantId)
        return 0
    }

    override fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int {
        errorWrite("batchDeleteByTenantIds", tenantIds)
        return 0
    }
}
