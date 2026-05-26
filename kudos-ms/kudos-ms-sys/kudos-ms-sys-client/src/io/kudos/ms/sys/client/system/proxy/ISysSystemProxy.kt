package io.kudos.ms.sys.client.system.proxy

import io.kudos.ms.sys.client.system.fallback.SysSystemFallback
import io.kudos.ms.sys.common.system.api.ISysSystemApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * System client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-system", fallback = SysSystemFallback::class)
interface ISysSystemProxy : ISysSystemApi {



}