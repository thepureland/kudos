package io.kudos.ms.sys.api.internal.controller.microservice

import io.kudos.ms.sys.common.microservice.api.ISysMicroServiceApi
import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.api.SysMicroServiceApi
import org.springframework.web.bind.annotation.RestController


/**
 * 微服务 内部 RPC 控制器。路径继承自 [ISysMicroServiceApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysMicroServiceInternalController(
    private val sysMicroServiceApi: SysMicroServiceApi,
) : ISysMicroServiceApi {

    override fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry? =
        sysMicroServiceApi.getMicroServiceFromCache(code)

    override fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry> =
        sysMicroServiceApi.getAllMicroServicesFromCache()

    override fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry> =
        sysMicroServiceApi.getMicroServicesExcludeAtomicFromCache()

    override fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry> =
        sysMicroServiceApi.getAtomicServicesFromCache()

    override fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry> =
        sysMicroServiceApi.getSubMicroServicesFromCache(parentCode)

    override fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry> =
        sysMicroServiceApi.getAtomicServicesByParentCodeFromCache(parentCode)

    override fun updateActive(code: String, active: Boolean): Boolean =
        sysMicroServiceApi.updateActive(code, active)

}
