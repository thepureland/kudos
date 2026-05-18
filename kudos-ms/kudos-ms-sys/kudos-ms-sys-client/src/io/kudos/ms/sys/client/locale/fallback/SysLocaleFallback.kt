package io.kudos.ms.sys.client.locale.fallback

import io.kudos.ms.sys.client.locale.proxy.ISysLocaleProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import org.springframework.stereotype.Component


/**
 * 语言/区域字典 Feign 容错降级实现
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
