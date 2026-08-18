package io.kudos.ms.sys.client.param.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.sys.client.param.proxy.ISysParamProxy
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import org.springframework.stereotype.Component


/**
 * Param Feign client fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysParamFallback : AbstractFeignFallbackSupport("SysParamFallback"), ISysParamProxy {

    override fun getParam(paramName: String, atomicServiceCode: String): SysParamCacheEntry? {
        warnRead("getParam", paramName, atomicServiceCode)
        return null
    }
}
