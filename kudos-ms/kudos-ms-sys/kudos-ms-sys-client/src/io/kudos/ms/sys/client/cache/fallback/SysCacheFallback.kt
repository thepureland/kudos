package io.kudos.ms.sys.client.cache.fallback

import io.kudos.ms.sys.client.cache.proxy.ISysCacheProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import org.springframework.stereotype.Component


/**
 * Cache Feign fallback implementation. `ISysCacheApi` currently defines no public methods; the class is kept as a valid `@FeignClient(fallback=...)` target.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysCacheFallback : SysClientFallbackSupport("SysCacheFallback"), ISysCacheProxy
