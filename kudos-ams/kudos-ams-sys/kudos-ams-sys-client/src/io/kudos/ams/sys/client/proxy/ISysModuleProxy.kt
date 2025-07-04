package io.kudos.ams.sys.client.proxy

import io.kudos.ams.sys.common.api.ISysModuleApi
import io.kudos.ams.sys.client.fallback.SysModuleFallback
import org.springframework.cloud.openfeign.FeignClient


/**
 * 模块客户端代理接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@FeignClient(name = "sys-module", fallback = SysModuleFallback::class)
interface ISysModuleProxy : ISysModuleApi {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}