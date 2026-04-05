package io.kudos.ms.sys.core.system.api
import io.kudos.ms.sys.common.system.api.ISysSystemApi
import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import io.kudos.ms.sys.core.system.service.iservice.ISysSystemService
import org.springframework.stereotype.Service


/**
 * 系统 API本地实现
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
open class SysSystemApi(
    private val sysSystemService: ISysSystemService,
) : ISysSystemApi {

    override fun getSystemFromCache(code: String): SysSystemCacheEntry? = sysSystemService.getSystemFromCache(code)

    override fun getAllSystemsFromCache(): List<SysSystemCacheEntry> = sysSystemService.getAllSystemsFromCache()

    override fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry> =
        sysSystemService.getSystemsExcludeSubSystemFromCache()

    override fun updateActive(code: String, active: Boolean): Boolean = sysSystemService.updateActive(code, active)

    override fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry> =
        sysSystemService.getSubSystemsFromCache(systemCode)
}
