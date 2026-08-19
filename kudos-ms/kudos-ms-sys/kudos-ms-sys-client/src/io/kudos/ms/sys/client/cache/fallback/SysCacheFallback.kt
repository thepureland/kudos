package io.kudos.ms.sys.client.cache.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.cache.proxy.ISysCacheProxy


/**
 * Cache fallback implementation. `ISysCacheApi` currently defines no public methods; the class is kept as a valid `@HttpServiceFallback` target.
 *
 * @author K
 * @since 1.0.0
 */
open class SysCacheFallback : AbstractHttpFallbackSupport("SysCacheFallback"), ISysCacheProxy
