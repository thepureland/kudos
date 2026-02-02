package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.core.model.po.SysAccessRule
import io.kudos.ms.sys.common.vo.accessrule.SysAccessRuleRecord


/**
 * 访问规则业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysAccessRuleService : IBaseCrudService<String, SysAccessRule> {
//endregion your codes 1

    //region your codes 2

    /**
     * 获取访问规则
     *
     * @param tenantId 租户id，可以为null
     * @param systemCode 系统编码
     * @return 访问规则记录，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getAccessRuleByTenantAndSystem(tenantId: String?, systemCode: String): SysAccessRuleRecord?

    /**
     * 获取租户的访问规则列表
     *
     * @param tenantId 租户id
     * @return 访问规则记录列表
     * @author K
     * @since 1.0.0
     */
    fun getAccessRulesByTenantId(tenantId: String): List<SysAccessRuleRecord>

    /**
     * 获取系统的访问规则列表
     *
     * @param systemCode 系统编码
     * @return 访问规则记录列表
     * @author K
     * @since 1.0.0
     */
    fun getAccessRulesBySystemCode(systemCode: String): List<SysAccessRuleRecord>

    /**
     * 更新启用状态
     *
     * @param id 访问规则id
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    //endregion your codes 2

}
