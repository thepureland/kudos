package io.kudos.ms.sys.common.tenant.api

import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * 租户 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysTenantApi {

    /**
     * 返回指定 id 的租户（从缓存，含未启用）
     */
    @GetMapping("/api/internal/sys/tenant/getTenant")
    fun getTenantFromCache(@RequestParam id: String): SysTenantCacheEntry?

    /**
     * 根据 id 集合取得对应租户信息并缓存（含未启用）
     */
    @PostMapping("/api/internal/sys/tenant/getTenantsByIds")
    fun getTenantsFromCacheByIds(@RequestBody ids: Collection<String>): Map<String, SysTenantCacheEntry>

    /**
     * 返回指定子系统下的租户（仅启用；内部先取子系统关联租户再按 `active` 过滤）
     */
    @GetMapping("/api/internal/sys/tenant/getTenantsBySubSystemCode")
    fun getTenantsBySubSystemCode(@RequestParam subSystemCode: String): List<SysTenantCacheEntry>


}
