package io.kudos.ams.sys.client.proxy

import io.kudos.ams.sys.common.api.ISysMicroServiceAtomicServiceApi
import io.kudos.ams.sys.client.fallback.SysMicroServiceAtomicServiceFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 微服务-原子服务关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-microserviceatomicservice", fallback = SysMicroServiceAtomicServiceFallback::class)
interface ISysMicroServiceAtomicServiceProxy : ISysMicroServiceAtomicServiceApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}