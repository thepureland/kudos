package io.kudos.ms.sys.client.locale.fallback

import io.kudos.ms.sys.client.locale.proxy.ISysLocaleProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import org.springframework.stereotype.Component


/**
 * Locale dictionary Feign client fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysLocaleFallback : SysClientFallbackSupport("SysLocaleFallback"), ISysLocaleProxy {

    override fun getLocaleByCode(code: String): SysLocaleCacheEntry? {
        warnRead("getLocaleByCode", code)
        return null
    }

    override fun listActiveLocales(): List<SysLocaleCacheEntry> {
        warnRead("listActiveLocales")
        return emptyList()
    }
}
