package io.kudos.ms.sys.common.system.api

import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * 系统 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysSystemApi {

    @GetMapping("/api/internal/sys/system/getSystem")
    fun getSystemFromCache(@RequestParam code: String): SysSystemCacheEntry?

    @GetMapping("/api/internal/sys/system/listAll")
    fun getAllSystemsFromCache(): List<SysSystemCacheEntry>

    @GetMapping("/api/internal/sys/system/listExcludeSubSystem")
    fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry>

    @PutMapping("/api/internal/sys/system/updateActive")
    fun updateActive(@RequestParam code: String, @RequestParam active: Boolean): Boolean

    @GetMapping("/api/internal/sys/system/listSubSystems")
    fun getSubSystemsFromCache(@RequestParam systemCode: String): List<SysSystemCacheEntry>


}
