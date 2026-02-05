package io.kudos.ms.sys.client.proxy

import io.kudos.ms.sys.client.fallback.SysSubSystemMicroServiceFallback
import io.kudos.ms.sys.common.api.ISysSubSystemMicroServiceApi
import org.springframework.cloud.openfeign.FeignClient


/**
 * 子系统-微服务关系客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-subsystemmicroservice", fallback = SysSubSystemMicroServiceFallback::class)
interface ISysSubSystemMicroServiceProxy : ISysSubSystemMicroServiceApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}