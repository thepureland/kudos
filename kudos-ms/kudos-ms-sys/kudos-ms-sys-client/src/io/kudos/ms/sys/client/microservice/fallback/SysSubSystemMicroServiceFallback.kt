package io.kudos.ms.sys.client.microservice.fallback

import io.kudos.ms.sys.client.microservice.proxy.ISysSubSystemMicroServiceProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import org.springframework.stereotype.Component


/**
 * SubSystem-Microservice relation Feign client fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysSubSystemMicroServiceFallback :
    SysClientFallbackSupport("SysSubSystemMicroServiceFallback"), ISysSubSystemMicroServiceProxy {

    override fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> {
        warnRead("getMicroServiceCodesBySubSystemCode", subSystemCode)
        return emptySet()
    }

    override fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> {
        warnRead("getSubSystemCodesByMicroServiceCode", microServiceCode)
        return emptySet()
    }

    override fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int {
        errorWrite("batchBind", subSystemCode, microServiceCodes)
        return 0
    }

    override fun unbind(subSystemCode: String, microServiceCode: String): Boolean {
        errorWrite("unbind", subSystemCode, microServiceCode)
        return false
    }

    override fun exists(subSystemCode: String, microServiceCode: String): Boolean {
        warnRead("exists", subSystemCode, microServiceCode)
        return false
    }
}
