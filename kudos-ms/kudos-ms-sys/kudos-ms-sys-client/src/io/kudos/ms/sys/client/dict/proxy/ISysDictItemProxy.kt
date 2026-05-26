package io.kudos.ms.sys.client.dict.proxy

import io.kudos.ms.sys.client.dict.fallback.SysDictItemFallback
import io.kudos.ms.sys.common.dict.api.ISysDictItemApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * Dictionary item client proxy interface.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient(name = "sys-dictitem", fallback = SysDictItemFallback::class)
interface ISysDictItemProxy : ISysDictItemApi {



}