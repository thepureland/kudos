package io.kudos.ams.sys.consumer.proxy

import io.kudos.ams.sys.common.api.ISysDictApi
import io.kudos.ams.sys.consumer.fallback.SysDictFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 字典客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-dict", fallback = SysDictFallback::class)
interface ISysDictProxy : ISysDictApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}