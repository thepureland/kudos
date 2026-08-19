package io.kudos.ms.sys.client.outline.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.outline.proxy.ISysOutLineProxy
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry


/**
 * Outbound network whitelist fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
open class SysOutLineFallback : AbstractHttpFallbackSupport("SysOutLineFallback"), ISysOutLineProxy {

    override fun listOutLines(systemCode: String, tenantId: String?): List<SysOutLineCacheEntry> {
        warnRead("listOutLines", systemCode, tenantId)
        return emptyList()
    }
}
