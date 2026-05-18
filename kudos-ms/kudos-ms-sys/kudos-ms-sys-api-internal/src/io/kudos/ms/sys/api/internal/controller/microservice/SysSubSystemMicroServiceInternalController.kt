package io.kudos.ms.sys.api.internal.controller.microservice

import io.kudos.ms.sys.common.microservice.api.ISysSubSystemMicroServiceApi
import io.kudos.ms.sys.core.microservice.api.SysSubSystemMicroServiceApi
import org.springframework.web.bind.annotation.RestController


/**
 * 子系统-微服务 关联 内部 RPC 控制器。路径继承自 [ISysSubSystemMicroServiceApi] 方法级注解。
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
