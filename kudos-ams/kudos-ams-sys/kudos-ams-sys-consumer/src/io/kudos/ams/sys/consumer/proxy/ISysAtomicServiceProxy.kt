package io.kudos.ams.sys.consumer.proxy

import io.kudos.ams.sys.common.api.ISysAtomicServiceApi
import io.kudos.ams.sys.consumer.fallback.SysAtomicServiceFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 原子服务客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-atomicservice", fallback = SysAtomicServiceFallback::class)
interface ISysAtomicServiceProxy : ISysAtomicServiceApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}