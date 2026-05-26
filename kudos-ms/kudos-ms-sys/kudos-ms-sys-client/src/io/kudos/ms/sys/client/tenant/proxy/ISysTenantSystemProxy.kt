package io.kudos.ms.sys.client.tenant.proxy

import io.kudos.ms.sys.client.tenant.fallback.SysTenantSystemFallback
import io.kudos.ms.sys.common.tenant.api.ISysTenantSystemApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Tenant-subsystem relationship client proxy interface
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-tenantsystem", fallback = SysTenantSystemFallback::class)
interface ISysTenantSystemProxy : ISysTenantSystemApi {



}