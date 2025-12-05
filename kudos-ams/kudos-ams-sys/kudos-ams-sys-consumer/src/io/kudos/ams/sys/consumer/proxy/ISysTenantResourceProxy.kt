package io.kudos.ams.sys.consumer.proxy

import io.kudos.ams.sys.common.api.ISysTenantResourceApi
import io.kudos.ams.sys.consumer.fallback.SysTenantResourceFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 租户-资源关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-tenantresource", fallback = SysTenantResourceFallback::class)
interface ISysTenantResourceProxy : ISysTenantResourceApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}