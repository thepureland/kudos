package io.kudos.ms.sys.client.cache.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.sys.client.cache.proxy.ISysCacheProxy
import org.springframework.stereotype.Component


/**
 * Cache Feign fallback implementation. `ISysCacheApi` currently defines no public methods; the class is kept as a valid `@FeignClient(fallback=...)` target.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysCacheFallback : AbstractFeignFallbackSupport("SysCacheFallback"), ISysCacheProxy
