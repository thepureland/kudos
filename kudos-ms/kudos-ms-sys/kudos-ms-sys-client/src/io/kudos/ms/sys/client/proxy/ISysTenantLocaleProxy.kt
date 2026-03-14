package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysTenantLocaleFallback
import io.kudos.ms.sys.common.api.ISysTenantLocaleApi
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