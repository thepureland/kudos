package io.kudos.ms.sys.api.internal.controller.microservice

import io.kudos.ms.sys.common.microservice.api.ISysSubSystemMicroServiceApi
import io.kudos.ms.sys.core.microservice.api.SysSubSystemMicroServiceApi
import org.springframework.web.bind.annotation.RestController


/**
 * Sub-system to microservice association internal RPC controller. Paths are inherited from method-level annotations on [ISysSubSystemMicroServiceApi].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysSubSystemMicroServiceInternalController(
    private val sysSubSystemMicroServiceApi: SysSubSystemMicroServiceApi,
) : ISysSubSystemMicroServiceApi {

    override fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> =
        sysSubSystemMicroServiceApi.getMicroServiceCodesBySubSystemCode(subSystemCode)

    override fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> =
        sysSubSystemMicroServiceApi.getSubSystemCodesByMicroServiceCode(microServiceCode)

    override fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int =
        sysSubSystemMicroServiceApi.batchBind(subSystemCode, microServiceCodes)

    override fun unbind(subSystemCode: String, microServiceCode: String): Boolean =
        sysSubSystemMicroServiceApi.unbind(subSystemCode, microServiceCode)

    override fun exists(subSystemCode: String, microServiceCode: String): Boolean =
        sysSubSystemMicroServiceApi.exists(subSystemCode, microServiceCode)

}
