package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysParamFallback
import io.kudos.ms.sys.common.api.ISysParamApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 参数客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-param", fallback = SysParamFallback::class)
interface ISysParamProxy : ISysParamApi {



}