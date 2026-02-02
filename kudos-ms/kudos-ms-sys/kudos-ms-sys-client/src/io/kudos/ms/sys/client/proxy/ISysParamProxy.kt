package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.common.api.ISysParamApi
import io.kudos.ms.sys.client.fallback.SysParamFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 参数客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-param", fallback = SysParamFallback::class)
interface ISysParamProxy : ISysParamApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}