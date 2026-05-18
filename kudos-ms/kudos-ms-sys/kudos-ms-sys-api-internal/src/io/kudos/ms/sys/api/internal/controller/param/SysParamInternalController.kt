package io.kudos.ms.sys.api.internal.controller.param

import io.kudos.ms.sys.common.param.api.ISysParamApi
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.core.param.api.SysParamApi
import org.springframework.web.bind.annotation.RestController


/**
 * 参数 内部 RPC 控制器。
 * 路径与 HTTP 方法直接继承自 [ISysParamApi] 的方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysParamInternalController(
    private val sysParamApi: SysParamApi,
) : ISysParamApi {

    override fun getParam(paramName: String, atomicServiceCode: String): SysParamCacheEntry? =
        sysParamApi.getParam(paramName, atomicServiceCode)

}
