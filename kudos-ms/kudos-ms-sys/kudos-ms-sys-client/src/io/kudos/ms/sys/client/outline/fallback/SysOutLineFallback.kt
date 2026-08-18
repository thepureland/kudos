package io.kudos.ms.sys.client.outline.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.sys.client.outline.proxy.ISysOutLineProxy
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import org.springframework.stereotype.Component


/**
 * Outbound network whitelist Feign client fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysOutLineFallback : AbstractFeignFallbackSupport("SysOutLineFallback"), ISysOutLineProxy {

    override fun listOutLines(systemCode: String, tenantId: String?): List<SysOutLineCacheEntry> {
        warnRead("listOutLines", systemCode, tenantId)
        return emptyList()
    }
}
