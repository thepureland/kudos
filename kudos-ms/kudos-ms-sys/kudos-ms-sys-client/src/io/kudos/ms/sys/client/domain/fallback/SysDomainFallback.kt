package io.kudos.ms.sys.client.domain.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.sys.client.domain.proxy.ISysDomainProxy
import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import org.springframework.stereotype.Component


/**
 * Domain Feign client fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDomainFallback : AbstractFeignFallbackSupport("SysDomainFallback"), ISysDomainProxy {

    override fun getDomainByName(domainName: String): SysDomainCacheEntry? {
        warnRead("getDomainByName", domainName)
        return null
    }
}
