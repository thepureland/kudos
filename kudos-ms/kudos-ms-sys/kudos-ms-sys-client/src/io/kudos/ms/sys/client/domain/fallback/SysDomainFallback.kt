package io.kudos.ms.sys.client.domain.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.domain.proxy.ISysDomainProxy
import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry


/**
 * Domain fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
open class SysDomainFallback : AbstractHttpFallbackSupport("SysDomainFallback"), ISysDomainProxy {

    override fun getDomainByName(domainName: String): SysDomainCacheEntry? {
        warnRead("getDomainByName", domainName)
        return null
    }
}
