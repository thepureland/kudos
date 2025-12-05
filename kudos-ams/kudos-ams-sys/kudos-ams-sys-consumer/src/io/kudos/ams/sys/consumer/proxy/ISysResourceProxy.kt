package io.kudos.ams.sys.consumer.proxy

import io.kudos.ams.sys.common.api.ISysResourceApi
import io.kudos.ams.sys.consumer.fallback.SysResourceFallback
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