package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysResourceFallback
import io.kudos.ms.sys.common.api.ISysResourceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 资源客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-resource", fallback = SysResourceFallback::class)
interface ISysResourceProxy : ISysResourceApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}