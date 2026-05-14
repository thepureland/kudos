package io.kudos.ms.sys.client.tenant.fallback

import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.client.tenant.proxy.ISysTenantResourceProxy
import org.springframework.stereotype.Component


/**
 * 租户-资源关系 Feign 容错降级实现。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysTenantResourceFallback : SysClientFallbackSupport("SysTenantResourceFallback"), ISysTenantResourceProxy {

    override fun getResourceIdsByTenantId(tenantId: String): Set<String> {
        warnRead("getResourceIdsByTenantId", tenantId)
        return emptySet()
    }

    override fun getTenantIdsByResourceId(resourceId: String): Set<String> {
        warnRead("getTenantIdsByResourceId", resourceId)
        return emptySet()
    }

    override fun batchBind(tenantId: String, resourceIds: Collection<String>): Int {
        errorWrite("batchBind", tenantId, resourceIds)
        return 0
    }

    override fun unbind(tenantId: String, resourceId: String): Boolean {
        errorWrite("unbind", tenantId, resourceId)
        return false
    }

    override fun exists(tenantId: String, resourceId: String): Boolean {
        // 安全默认：远端不可达时按「未授权」处理
        warnRead("exists", tenantId, resourceId)
        return false
    }
}
