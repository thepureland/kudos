package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysDictFallback
import io.kudos.ms.sys.common.api.ISysDictApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 字典客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-dict", fallback = SysDictFallback::class)
interface ISysDictProxy : ISysDictApi {



}