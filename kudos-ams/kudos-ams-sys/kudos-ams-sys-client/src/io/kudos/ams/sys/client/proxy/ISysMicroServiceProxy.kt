package io.kudos.ams.sys.client.proxy

import io.kudos.ams.sys.common.api.ISysMicroServiceApi
import io.kudos.ams.sys.client.fallback.SysMicroServiceFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 微服务客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-microservice", fallback = SysMicroServiceFallback::class)
interface ISysMicroServiceProxy : ISysMicroServiceApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}