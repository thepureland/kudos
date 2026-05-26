package io.kudos.ms.sys.client.outline.proxy

import io.kudos.ms.sys.client.outline.fallback.SysOutLineFallback
import io.kudos.ms.sys.common.outline.api.ISysOutLineApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Outbound network whitelist client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-out-line", fallback = SysOutLineFallback::class)
interface ISysOutLineProxy : ISysOutLineApi
