package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.accessrule.SysAccessRuleRecord


/**
 * 访问规则 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysAccessRuleApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 获取访问规则
     *
     * @param tenantId 租户id，可以为null
     * @param systemCode 系统编码
     * @return 访问规则记录，找不到返回null
     */
    fun getAccessRuleByTenantAndSystem(tenantId: String?, systemCode: String): SysAccessRuleRecord?

    /**
     * 更新启用状态
     *
     * @param id 访问规则id
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(id: String, active: Boolean): Boolean

    //endregion your codes 2

}