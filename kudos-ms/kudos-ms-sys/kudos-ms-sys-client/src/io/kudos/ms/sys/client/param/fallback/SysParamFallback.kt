package io.kudos.ms.sys.client.param.fallback

import io.kudos.ms.sys.client.param.proxy.ISysParamProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import org.springframework.stereotype.Component


/**
 * 参数 Feign 容错降级实现。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysParamFallback : SysClientFallbackSupport("SysParamFallback"), ISysParamProxy {

    override fun getParam(paramName: String, atomicServiceCode: String): SysParamCacheEntry? {
        warnRead("getParam", paramName, atomicServiceCode)
        return null
    }
}
