package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysSubSystemMicroServiceApi
import io.kudos.ms.sys.core.service.iservice.ISysSubSystemMicroServiceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 子系统-微服务关系 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysSubSystemMicroServiceApi : ISysSubSystemMicroServiceApi {


    @Resource
    protected lateinit var sysSubSystemMicroServiceService: ISysSubSystemMicroServiceService

    override fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> {
        return sysSubSystemMicroServiceService.getMicroServiceCodesBySubSystemCode(subSystemCode)
    }

    override fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> {
        return sysSubSystemMicroServiceService.getSubSystemCodesByMicroServiceCode(microServiceCode)
    }

    override fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int {
        return sysSubSystemMicroServiceService.batchBind(subSystemCode, microServiceCodes)
    }

    override fun unbind(subSystemCode: String, microServiceCode: String): Boolean {
        return sysSubSystemMicroServiceService.unbind(subSystemCode, microServiceCode)
    }

    override fun exists(subSystemCode: String, microServiceCode: String): Boolean {
        return sysSubSystemMicroServiceService.exists(subSystemCode, microServiceCode)
    }


}