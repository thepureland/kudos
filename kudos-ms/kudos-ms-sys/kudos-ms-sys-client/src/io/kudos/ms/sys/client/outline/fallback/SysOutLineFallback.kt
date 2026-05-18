package io.kudos.ms.sys.client.outline.fallback

import io.kudos.ms.sys.client.outline.proxy.ISysOutLineProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import org.springframework.stereotype.Component


/**
 * 出网白名单 Feign 容错降级实现
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysOutLineFallback : SysClientFallbackSupport("SysOutLineFallback"), ISysOutLineProxy {

    override fun listOutLines(systemCode: String, tenantId: String?): List<SysOutLineCacheEntry> {
        warnRead("listOutLines", systemCode, tenantId)
        return emptyList()
    }
}
