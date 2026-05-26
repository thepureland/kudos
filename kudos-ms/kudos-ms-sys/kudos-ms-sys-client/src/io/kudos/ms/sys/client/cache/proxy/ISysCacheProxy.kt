package io.kudos.ms.sys.client.cache.proxy

import io.kudos.ms.sys.client.cache.fallback.SysCacheFallback
import io.kudos.ms.sys.common.cache.api.ISysCacheApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Cache client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-cache", fallback = SysCacheFallback::class)
interface ISysCacheProxy : ISysCacheApi {



}