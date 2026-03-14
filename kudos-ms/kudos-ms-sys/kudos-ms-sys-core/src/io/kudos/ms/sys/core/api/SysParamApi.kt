package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysParamApi
import io.kudos.ms.sys.common.vo.param.SysParamCacheEntry
import io.kudos.ms.sys.core.service.iservice.ISysParamService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 参数 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysParamApi : ISysParamApi {


    @Resource
    protected lateinit var sysParamService: ISysParamService

    override fun getParam(
        paramName: String,
        atomicServiceCode: String
    ): SysParamCacheEntry? {
        return sysParamService.getParam(paramName, atomicServiceCode)
    }


}