package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysMicroServiceApi
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheItem
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceRecord
import io.kudos.ms.sys.core.service.iservice.ISysMicroServiceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 微服务 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Component
open class SysMicroServiceApi : ISysMicroServiceApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysMicroServiceService: ISysMicroServiceService

    override fun getAllActiveMicroService(): List<SysMicroServiceCacheItem> {
        return sysMicroServiceService.getAllActiveMicroService()
    }

    override fun getAllActiveMicroServiceExcludeAtomicService(): List<SysMicroServiceCacheItem> {
        return sysMicroServiceService.getAllActiveMicroServiceExcludeAtomicService()
    }

    override fun getAllActiveAtomicService(): List<SysMicroServiceCacheItem> {
        return sysMicroServiceService.getAllActiveAtomicService()
    }

    override fun getMicroServiceByCode(code: String): SysMicroServiceCacheItem? {
        return sysMicroServiceService.getMicroServiceByCode(code)
    }

    override fun getAllActiveAtomicServiceByParentCode(parentCode: String): List<SysMicroServiceRecord> {
        return sysMicroServiceService.getAllActiveAtomicServiceByParentCode(parentCode)
    }

    override fun updateActive(code: String, active: Boolean): Boolean {
        return sysMicroServiceService.updateActive(code, active)
    }

    //endregion your codes 2

}