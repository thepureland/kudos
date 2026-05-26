package io.kudos.ms.sys.common.outline.api

import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * External API for outbound whitelist.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysOutLineApi {

    /**
     * Returns all enabled outbound whitelist entries for the specified system and tenant.
     * `tenantId == null` indicates platform-level rules.
     *
     * @param systemCode System code
     * @param tenantId Tenant id; when `null`, query platform-level rules.
     * @return List of outbound whitelist entries (only includes records with active=true).
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/sys/outLine/listOutLines")
    fun listOutLines(
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String? = null
    ): List<SysOutLineCacheEntry>

}
