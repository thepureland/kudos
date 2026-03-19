package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysMicroServiceApi
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheEntry
import io.kudos.ms.sys.common.vo.microservice.response.SysMicroServiceRow
import io.kudos.ms.sys.core.service.iservice.ISysMicroServiceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 微服务 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysMicroServiceApi : ISysMicroServiceApi {


    @Resource
    protected lateinit var sysMicroServiceService: ISysMicroServiceService

    override fun getAllActiveMicroService(): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceService.getAllActiveMicroServices()
    }

    override fun getAllActiveMicroServiceExcludeAtomicService(): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceService.getAllActiveMicroServiceExcludeAtomicService()
    }

    override fun getAllActiveAtomicService(): List<SysMicroServiceCacheEntry> {
        return sysMicroServiceService.getAllActiveAtomicServices()
    }

    override fun getMicroServiceByCode(code: String): SysMicroServiceCacheEntry? {
        return sysMicroServiceService.getMicroServiceByCode(code)
    }

    override fun getAllActiveAtomicServiceByParentCode(parentCode: String): List<SysMicroServiceRow> {
        return sysMicroServiceService.getAllActiveAtomicServiceByParentCode(parentCode)
    }

    override fun updateActive(code: String, active: Boolean): Boolean {
        return sysMicroServiceService.updateActive(code, active)
    }


}
