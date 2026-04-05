package io.kudos.ms.sys.client.resource.proxy

import io.kudos.ms.sys.client.resource.fallback.SysResourceFallback
import io.kudos.ms.sys.common.resource.api.ISysResourceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 资源客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-resource", fallback = SysResourceFallback::class)
interface ISysResourceProxy : ISysResourceApi {



}