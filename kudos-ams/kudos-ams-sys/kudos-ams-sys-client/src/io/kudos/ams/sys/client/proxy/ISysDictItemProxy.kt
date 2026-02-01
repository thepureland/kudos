package io.kudos.ams.sys.client.proxy

import io.kudos.ams.sys.common.api.ISysDictItemApi
import io.kudos.ams.sys.client.fallback.SysDictItemFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 字典项客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-dictitem", fallback = SysDictItemFallback::class)
interface ISysDictItemProxy : ISysDictItemApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}