package io.kudos.ams.sys.consumer.proxy

import io.kudos.ams.sys.common.api.ISysParamApi
import io.kudos.ams.sys.consumer.fallback.SysParamFallback
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