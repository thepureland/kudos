package io.kudos.ms.sys.client.system.fallback

import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.client.system.proxy.ISysSystemProxy
import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import org.springframework.stereotype.Component


/**
 * System Feign client fallback implementation.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysSystemFallback : SysClientFallbackSupport("SysSystemFallback"), ISysSystemProxy {

    override fun getSystemFromCache(code: String): SysSystemCacheEntry? {
        warnRead("getSystemFromCache", code)
        return null
    }

    override fun getAllSystemsFromCache(): List<SysSystemCacheEntry> {
        warnRead("getAllSystemsFromCache")
        return emptyList()
    }

    override fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry> {
        warnRead("getSystemsExcludeSubSystemFromCache")
        return emptyList()
    }

    override fun updateActive(code: String, active: Boolean): Boolean {
        errorWrite("updateActive", code, active)
        return false
    }

    override fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry> {
        warnRead("getSubSystemsFromCache", systemCode)
        return emptyList()
    }
}
