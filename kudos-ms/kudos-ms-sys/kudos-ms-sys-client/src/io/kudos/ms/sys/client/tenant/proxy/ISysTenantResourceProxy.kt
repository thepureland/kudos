package io.kudos.ms.sys.client.tenant.proxy

import io.kudos.ms.sys.client.tenant.fallback.SysTenantResourceFallback
import io.kudos.ms.sys.common.tenant.api.ISysTenantResourceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 租户-资源关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-tenantresource", fallback = SysTenantResourceFallback::class)
interface ISysTenantResourceProxy : ISysTenantResourceApi {



}