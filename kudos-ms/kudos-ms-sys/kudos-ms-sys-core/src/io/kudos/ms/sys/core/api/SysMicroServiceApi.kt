package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysMicroServiceApi
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.service.iservice.ISysMicroServiceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 微服务 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysMicroServiceApi : ISysMicroServiceApi {


    @Resource
    protected lateinit var sysMicroServiceService: ISysMicroServiceService

    override fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry? {
        return sysMicroServiceService.getMicroServiceFromCache(code)
    }

    override fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceService.getAllMicroServicesFromCache()
    }

    override fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceService.getMicroServicesExcludeAtomicFromCache()
    }

    override fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceService.getAtomicServicesFromCache()
    }

    override fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceService.getSubMicroServicesFromCache(parentCode)
    }

    override fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceService.getAtomicServicesByParentCodeFromCache(parentCode)
    }

    override fun updateActive(code: String, active: Boolean): Boolean {
        return sysMicroServiceService.updateActive(code, active)
    }


}
