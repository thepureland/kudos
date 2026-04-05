package io.kudos.ms.sys.client.dict.proxy

import io.kudos.ms.sys.client.dict.fallback.SysDictFallback
import io.kudos.ms.sys.common.dict.api.ISysDictApi
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