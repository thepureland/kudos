package io.kudos.ams.sys.consumer.proxy

import io.kudos.ams.sys.common.api.ISysTenantSubSystemApi
import io.kudos.ams.sys.consumer.fallback.SysTenantSubSystemFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 租户-子系统关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-tenantsubsystem", fallback = SysTenantSubSystemFallback::class)
interface ISysTenantSubSystemProxy : ISysTenantSubSystemApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}