package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysSystemApi
import io.kudos.ms.sys.common.vo.system.SysSystemCacheEntry
import io.kudos.ms.sys.core.service.iservice.ISysSystemService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 系统 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
open class SysSystemApi : ISysSystemApi {


    @Resource
    protected lateinit var sysSystemService: ISysSystemService

    override fun getSystemFromCache(code: String): SysSystemCacheEntry? {
        return sysSystemService.getSystemFromCache(code)
    }

    override fun getAllSystemsFromCache(): List<SysSystemCacheEntry> {
        return sysSystemService.getAllSystemsFromCache()
    }

    override fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry> {
        return sysSystemService.getSystemsExcludeSubSystemFromCache()
    }

    override fun updateActive(code: String, active: Boolean): Boolean {
        return sysSystemService.updateActive(code, active)
    }

    override fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry> {
        return sysSystemService.getSubSystemsFromCache(systemCode)
    }


}
