package io.kudos.ams.sys.client.proxy

import io.kudos.ams.sys.common.api.ISysPortalApi
import io.kudos.ams.sys.client.fallback.SysPortalFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 门户客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-portal", fallback = SysPortalFallback::class)
interface ISysPortalProxy : ISysPortalApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}