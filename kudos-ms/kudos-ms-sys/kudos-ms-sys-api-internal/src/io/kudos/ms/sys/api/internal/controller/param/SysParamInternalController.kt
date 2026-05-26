package io.kudos.ms.sys.api.internal.controller.param

import io.kudos.ms.sys.common.param.api.ISysParamApi
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.core.param.api.SysParamApi
import org.springframework.web.bind.annotation.RestController


/**
 * Parameter internal RPC controller.
 * Paths and HTTP methods are inherited directly from method-level annotations on [ISysParamApi].
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
