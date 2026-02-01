package io.kudos.ams.sys.client.proxy

import io.kudos.ams.sys.common.api.ISysSystemApi
import io.kudos.ams.sys.client.fallback.SysSystemFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 系统客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-system", fallback = SysSystemFallback::class)
interface ISysSystemProxy : ISysSystemApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}