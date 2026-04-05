package io.kudos.ms.sys.client.tenant.proxy

import io.kudos.ms.sys.client.tenant.fallback.SysTenantLocaleFallback
import io.kudos.ms.sys.common.tenant.api.ISysTenantLocaleApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 租户-语言关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-tenantlocale", fallback = SysTenantLocaleFallback::class)
interface ISysTenantLocaleProxy : ISysTenantLocaleApi {



}