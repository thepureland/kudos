package io.kudos.ms.sys.core.domain.api

import io.kudos.ms.sys.common.domain.api.ISysDomainApi
import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.core.domain.service.iservice.ISysDomainService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component


/**
 * Local implementation of the domain API
 *
 * @author K
 * @since 1.0.0
 */
@Primary
@Component
open class SysDomainApi(
    private val sysDomainService: ISysDomainService,
) : ISysDomainApi {

    override fun getDomainByName(domainName: String): SysDomainCacheEntry? = sysDomainService.getDomainFromCache(domainName)
}
