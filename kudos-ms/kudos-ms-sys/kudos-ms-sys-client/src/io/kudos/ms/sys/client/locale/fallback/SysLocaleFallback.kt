package io.kudos.ms.sys.client.locale.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.locale.proxy.ISysLocaleProxy
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry


/**
 * Locale dictionary fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
open class SysLocaleFallback : AbstractHttpFallbackSupport("SysLocaleFallback"), ISysLocaleProxy {

    override fun getLocaleByCode(code: String): SysLocaleCacheEntry? {
        warnRead("getLocaleByCode", code)
        return null
    }

    override fun listActiveLocales(): List<SysLocaleCacheEntry> {
        warnRead("listActiveLocales")
        return emptyList()
    }
}
