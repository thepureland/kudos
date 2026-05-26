package io.kudos.ms.sys.common.accessrule.api

import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * External access rule API.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAccessRuleApi {


    /**
     * Get an access rule.
     *
     * @param systemCode system code, defaults to "default"
     * @param tenantId tenant id, defaults to "default"
     * @return access rule record, or null if not found
     */
    @GetMapping("/api/internal/sys/accessRule/getAccessRule")
    fun getAccessRuleByTenantAndSystem(
        @RequestParam(required = false, defaultValue = "default") systemCode: String = "default",
        @RequestParam(required = false, defaultValue = "default") tenantId: String = "default"
    ): SysAccessRuleRow?

    /**
     * Update the enabled state.
     *
     * @param id access rule id
     * @param active whether enabled
     * @return whether the update succeeded
     */
    @PutMapping("/api/internal/sys/accessRule/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean


}
