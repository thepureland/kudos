package io.kudos.ms.sys.api.internal.controller.domain

import io.kudos.ms.sys.common.domain.api.ISysDomainApi
import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.core.domain.api.SysDomainApi
import org.springframework.web.bind.annotation.RestController


/**
 * 域名 内部 RPC 控制器。路径继承自 [ISysDomainApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysDomainInternalController(
    private val sysDomainApi: SysDomainApi,
) : ISysDomainApi {

    override fun getDomainByName(domainName: String): SysDomainCacheEntry? =
        sysDomainApi.getDomainByName(domainName)

}
