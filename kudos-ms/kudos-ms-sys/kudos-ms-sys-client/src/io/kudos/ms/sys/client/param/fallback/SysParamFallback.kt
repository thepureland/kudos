package io.kudos.ms.sys.client.param.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.param.proxy.ISysParamProxy
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry


/**
 * Param fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
open class SysParamFallback : AbstractHttpFallbackSupport("SysParamFallback"), ISysParamProxy {

    override fun getParam(paramName: String, atomicServiceCode: String): SysParamCacheEntry? {
        warnRead("getParam", paramName, atomicServiceCode)
        return null
    }
}
