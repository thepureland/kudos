package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysMicroServiceFallback
import io.kudos.ms.sys.common.api.ISysMicroServiceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 微服务客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-microservice", fallback = SysMicroServiceFallback::class)
interface ISysMicroServiceProxy : ISysMicroServiceApi {



}