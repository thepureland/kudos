package io.kudos.ms.sys.client.param.proxy

import io.kudos.ms.sys.client.param.fallback.SysParamFallback
import io.kudos.ms.sys.common.param.api.ISysParamApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Param client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-param", fallback = SysParamFallback::class)
interface ISysParamProxy : ISysParamApi {



}