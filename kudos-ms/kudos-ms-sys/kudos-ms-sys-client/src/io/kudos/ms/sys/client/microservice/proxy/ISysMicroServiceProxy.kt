package io.kudos.ms.sys.client.microservice.proxy

import io.kudos.ms.sys.client.microservice.fallback.SysMicroServiceFallback
import io.kudos.ms.sys.common.microservice.api.ISysMicroServiceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Microservice client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-microservice", fallback = SysMicroServiceFallback::class)
interface ISysMicroServiceProxy : ISysMicroServiceApi {



}