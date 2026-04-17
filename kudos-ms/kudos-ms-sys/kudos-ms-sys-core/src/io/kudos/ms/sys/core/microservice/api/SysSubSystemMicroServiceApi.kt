package io.kudos.ms.sys.core.microservice.api

import io.kudos.ms.sys.common.microservice.api.ISysSubSystemMicroServiceApi
import io.kudos.ms.sys.core.microservice.service.iservice.ISysSubSystemMicroServiceService
import org.springframework.stereotype.Service


/**
 * 子系统-微服务关系 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysSubSystemMicroServiceApi(
    private val sysSubSystemMicroServiceService: ISysSubSystemMicroServiceService,
) : ISysSubSystemMicroServiceApi {

    override fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> =
        sysSubSystemMicroServiceService.getMicroServiceCodesBySubSystemCode(subSystemCode)

    override fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> =
        sysSubSystemMicroServiceService.getSubSystemCodesByMicroServiceCode(microServiceCode)

    override fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int =
        sysSubSystemMicroServiceService.batchBind(subSystemCode, microServiceCodes)

    override fun unbind(subSystemCode: String, microServiceCode: String): Boolean =
        sysSubSystemMicroServiceService.unbind(subSystemCode, microServiceCode)

    override fun exists(subSystemCode: String, microServiceCode: String): Boolean =
        sysSubSystemMicroServiceService.exists(subSystemCode, microServiceCode)
}
