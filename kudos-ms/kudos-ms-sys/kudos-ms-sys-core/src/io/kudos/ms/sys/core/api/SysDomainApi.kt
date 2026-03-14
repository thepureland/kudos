package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDomainApi
import io.kudos.ms.sys.common.vo.domain.SysDomainCacheEntry
import io.kudos.ms.sys.core.service.iservice.ISysDomainService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 域名 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDomainApi : ISysDomainApi {


    @Resource
    protected lateinit var sysDomainService: ISysDomainService

    override fun getDomainByName(domainName: String): SysDomainCacheEntry? {
        return sysDomainService.getDomainByName(domainName)
    }


}