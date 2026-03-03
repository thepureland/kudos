package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysSystemApi
import io.kudos.ms.sys.common.vo.system.SysSystemCacheItem
import io.kudos.ms.sys.common.vo.system.SysSystemRecord
import io.kudos.ms.sys.core.service.iservice.ISysSystemService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 系统 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Service
open class SysSystemApi : ISysSystemApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysSystemService: ISysSystemService

    override fun getSystemByCode(code: String): SysSystemCacheItem? {
        return sysSystemService.getSystemByCode(code)
    }

    override fun getAllActiveSystems(): List<SysSystemRecord> {
        return sysSystemService.getAllActiveSystems()
    }

    override fun updateActive(code: String, active: Boolean): Boolean {
        return sysSystemService.updateActive(code, active)
    }

    override fun getSubSystemsBySystemCode(systemCode: String): List<SysSystemRecord> {
        return sysSystemService.getSubSystemsBySystemCode(systemCode)
    }

    //endregion your codes 2

}