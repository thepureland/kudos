package io.kudos.ms.sys.common.accessrule.api

import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * 访问规则 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAccessRuleApi {


    /**
     * 获取访问规则
     *
     * @param systemCode 系统编码，缺省为"default"
     * @param tenantId 租户id，缺省为"default"
     * @return 访问规则记录，找不到返回null
     */
    @GetMapping("/api/internal/sys/accessRule/getAccessRule")
    fun getAccessRuleByTenantAndSystem(
        @RequestParam(required = false, defaultValue = "default") systemCode: String = "default",
        @RequestParam(required = false, defaultValue = "default") tenantId: String = "default"
    ): SysAccessRuleRow?

    /**
     * 更新启用状态
     *
     * @param id 访问规则id
     * @param active 是否启用
     * @return 是否更新成功
     */
    @PutMapping("/api/internal/sys/accessRule/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean


}
