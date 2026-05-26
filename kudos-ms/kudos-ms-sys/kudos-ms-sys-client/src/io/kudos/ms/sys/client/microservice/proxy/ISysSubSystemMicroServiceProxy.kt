package io.kudos.ms.sys.client.microservice.proxy

import io.kudos.ms.sys.client.microservice.fallback.SysSubSystemMicroServiceFallback
import io.kudos.ms.sys.common.microservice.api.ISysSubSystemMicroServiceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * SubSystem-Microservice relation client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-subsystemmicroservice", fallback = SysSubSystemMicroServiceFallback::class)
interface ISysSubSystemMicroServiceProxy : ISysSubSystemMicroServiceApi {



}