package io.kudos.ms.sys.api.internal.controller.system

import io.kudos.ms.sys.common.system.api.ISysSystemApi
import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import io.kudos.ms.sys.core.system.api.SysSystemApi
import org.springframework.web.bind.annotation.RestController


/**
 * 系统 内部 RPC 控制器。路径继承自 [ISysSystemApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysSystemInternalController(
    private val sysSystemApi: SysSystemApi,
) : ISysSystemApi {

    override fun getSystemFromCache(code: String): SysSystemCacheEntry? =
        sysSystemApi.getSystemFromCache(code)

    override fun getAllSystemsFromCache(): List<SysSystemCacheEntry> =
        sysSystemApi.getAllSystemsFromCache()

    override fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry> =
        sysSystemApi.getSystemsExcludeSubSystemFromCache()

    override fun updateActive(code: String, active: Boolean): Boolean =
        sysSystemApi.updateActive(code, active)

    override fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry> =
        sysSystemApi.getSubSystemsFromCache(systemCode)

}
