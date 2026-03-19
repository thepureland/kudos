package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.accessrule.response.SysAccessRuleRow


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
     * @author K
     * @since 1.0.0
     */
    fun getAccessRuleByTenantAndSystem(
        systemCode: String = "default",
        tenantId: String = "default"
    ): SysAccessRuleRow?

    /**
     * 更新启用状态
     *
     * @param id 访问规则id
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(id: String, active: Boolean): Boolean


}
