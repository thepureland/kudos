package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.common.api.ISysCacheApi
import io.kudos.ms.sys.client.fallback.SysCacheFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 缓存客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-cache", fallback = SysCacheFallback::class)
interface ISysCacheProxy : ISysCacheApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}