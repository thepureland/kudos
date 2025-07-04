package io.kudos.ams.sys.client.proxy

import io.kudos.ams.sys.common.api.ISysSubSystemApi
import io.kudos.ams.sys.client.fallback.SysSubSystemFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 子系统客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-subsystem", fallback = SysSubSystemFallback::class)
interface ISysSubSystemProxy : ISysSubSystemApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}