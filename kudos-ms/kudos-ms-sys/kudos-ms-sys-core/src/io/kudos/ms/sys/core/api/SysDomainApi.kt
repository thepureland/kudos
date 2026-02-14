package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDomainApi
import io.kudos.ms.sys.common.vo.domain.SysDomainCacheItem
import io.kudos.ms.sys.core.service.iservice.ISysDomainService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 域名 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Component
open class SysDomainApi : ISysDomainApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysDomainService: ISysDomainService

    override fun getDomainByName(domainName: String): SysDomainCacheItem? {
        return sysDomainService.getDomainByName(domainName)
    }

    //endregion your codes 2

}