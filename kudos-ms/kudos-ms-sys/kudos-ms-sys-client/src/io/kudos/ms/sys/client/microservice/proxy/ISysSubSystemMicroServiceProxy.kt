package io.kudos.ms.sys.client.microservice.proxy

import io.kudos.ms.sys.client.microservice.fallback.SysSubSystemMicroServiceFallback
import io.kudos.ms.sys.common.microservice.api.ISysSubSystemMicroServiceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 子系统-微服务关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-subsystemmicroservice", fallback = SysSubSystemMicroServiceFallback::class)
interface ISysSubSystemMicroServiceProxy : ISysSubSystemMicroServiceApi {



}