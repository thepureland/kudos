package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysTenantFallback
import io.kudos.ms.sys.common.api.ISysTenantApi
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