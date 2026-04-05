package io.kudos.ms.sys.client.dict.proxy

import io.kudos.ms.sys.client.dict.fallback.SysDictItemFallback
import io.kudos.ms.sys.common.dict.api.ISysDictItemApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 字典项客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-dictitem", fallback = SysDictItemFallback::class)
interface ISysDictItemProxy : ISysDictItemApi {



}