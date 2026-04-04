package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDomainApi
import io.kudos.ms.sys.common.vo.domain.SysDomainCacheEntry
import io.kudos.ms.sys.core.service.iservice.ISysDomainService
import org.springframework.stereotype.Component


/**
 * 域名 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDomainApi(
    private val sysDomainService: ISysDomainService,
) : ISysDomainApi {

    override fun getDomainByName(domainName: String): SysDomainCacheEntry? = sysDomainService.getDomainFromCache(domainName)
}
