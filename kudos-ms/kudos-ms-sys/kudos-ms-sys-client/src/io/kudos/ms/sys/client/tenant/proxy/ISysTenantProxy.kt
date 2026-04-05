package io.kudos.ms.sys.client.tenant.proxy

import io.kudos.ms.sys.client.tenant.fallback.SysTenantFallback
import io.kudos.ms.sys.common.tenant.api.ISysTenantApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 租户客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-tenant", fallback = SysTenantFallback::class)
interface ISysTenantProxy : ISysTenantApi {



}