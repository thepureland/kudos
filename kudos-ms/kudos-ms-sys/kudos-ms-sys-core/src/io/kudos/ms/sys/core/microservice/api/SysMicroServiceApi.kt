package io.kudos.ms.sys.core.microservice.api
import io.kudos.ms.sys.common.microservice.api.ISysMicroServiceApi
import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.service.iservice.ISysMicroServiceService
import org.springframework.stereotype.Component


/**
 * 微服务 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysMicroServiceApi(
    private val sysMicroServiceService: ISysMicroServiceService,
) : ISysMicroServiceApi {

    override fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry? = sysMicroServiceService.getMicroServiceFromCache(code)

    override fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry> = sysMicroServiceService.getAllMicroServicesFromCache()

    override fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry> =
        sysMicroServiceService.getMicroServicesExcludeAtomicFromCache()

    override fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry> = sysMicroServiceService.getAtomicServicesFromCache()

    override fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry> =
        sysMicroServiceService.getSubMicroServicesFromCache(parentCode)

    override fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry> =
        sysMicroServiceService.getAtomicServicesByParentCodeFromCache(parentCode)

    override fun updateActive(code: String, active: Boolean): Boolean = sysMicroServiceService.updateActive(code, active)
}
