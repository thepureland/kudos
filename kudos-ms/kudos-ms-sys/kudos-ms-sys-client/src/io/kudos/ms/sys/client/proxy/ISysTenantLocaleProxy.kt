package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.common.api.ISysTenantLocaleApi
import io.kudos.ms.sys.client.fallback.SysTenantLocaleFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 租户-语言关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-tenantlocale", fallback = SysTenantLocaleFallback::class)
interface ISysTenantLocaleProxy : ISysTenantLocaleApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}