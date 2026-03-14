package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysSystemApi
import io.kudos.ms.sys.common.vo.system.SysSystemCacheEntry
import io.kudos.ms.sys.common.vo.system.SysSystemRow
import io.kudos.ms.sys.core.service.iservice.ISysSystemService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 系统 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysSystemApi : ISysSystemApi {


    @Resource
    protected lateinit var sysSystemService: ISysSystemService

    override fun getSystemByCode(code: String): SysSystemCacheEntry? {
        return sysSystemService.getSystemByCode(code)
    }

    override fun getAllActiveSystems(): List<SysSystemRow> {
        return sysSystemService.getAllActiveSystems()
    }

    override fun updateActive(code: String, active: Boolean): Boolean {
        return sysSystemService.updateActive(code, active)
    }

    override fun getSubSystemsBySystemCode(systemCode: String): List<SysSystemRow> {
        return sysSystemService.getSubSystemsBySystemCode(systemCode)
    }


}