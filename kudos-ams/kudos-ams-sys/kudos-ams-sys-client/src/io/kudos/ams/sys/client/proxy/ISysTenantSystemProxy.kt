package io.kudos.ams.sys.client.proxy

import io.kudos.ams.sys.common.api.ISysTenantSystemApi
import io.kudos.ams.sys.client.fallback.SysTenantSystemFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 租户-子系统关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-tenantsystem", fallback = SysTenantSystemFallback::class)
interface ISysTenantSystemProxy : ISysTenantSystemApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}