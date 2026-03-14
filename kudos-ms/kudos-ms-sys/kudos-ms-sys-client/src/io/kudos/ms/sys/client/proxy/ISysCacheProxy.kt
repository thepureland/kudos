package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysCacheFallback
import io.kudos.ms.sys.common.api.ISysCacheApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 缓存客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-cache", fallback = SysCacheFallback::class)
interface ISysCacheProxy : ISysCacheApi {



}