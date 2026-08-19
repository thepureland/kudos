package io.kudos.ms.sys.common.system.api

import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PutExchange
import org.springframework.web.bind.annotation.RequestParam


/**
 * External system API.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysSystemApi {

    @GetExchange("/api/internal/sys/system/getSystem")
    fun getSystemFromCache(@RequestParam code: String): SysSystemCacheEntry?

    @GetExchange("/api/internal/sys/system/listAll")
    fun getAllSystemsFromCache(): List<SysSystemCacheEntry>

    @GetExchange("/api/internal/sys/system/listExcludeSubSystem")
    fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry>

    @PutExchange("/api/internal/sys/system/updateActive")
    fun updateActive(@RequestParam code: String, @RequestParam active: Boolean): Boolean

    @GetExchange("/api/internal/sys/system/listSubSystems")
    fun getSubSystemsFromCache(@RequestParam systemCode: String): List<SysSystemCacheEntry>


}
