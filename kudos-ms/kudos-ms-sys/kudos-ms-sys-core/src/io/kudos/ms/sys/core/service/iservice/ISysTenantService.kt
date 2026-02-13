package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.common.api.ISysTenantApi
import io.kudos.ms.sys.common.vo.tenant.SysTenantRecord
import io.kudos.ms.sys.core.model.po.SysTenant


/**
 * 租户业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface ISysTenantService : IBaseCrudService<String, SysTenant>, ISysTenantApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 返回所有启用的租户
     *
     * @return Map(子系统代码，List(租户记录对象))
     * @author K
     * @since 1.0.0
     */
    fun getAllActiveTenants(): Map<String, List<SysTenantRecord>>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 主键
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 根据id获取租户记录（非缓存）
     *
     * @param id 主键
     * @return 租户记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getTenantRecord(id: String): SysTenantRecord?

    /**
     * 根据名称获取租户记录
     *
     * @param name 租户名称
     * @return 租户记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getTenantByName(name: String): SysTenantRecord?

    /**
     * 获取租户的子系统编码列表
     *
     * @param tenantId 租户id
     * @return 子系统编码集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getSubSystemCodesByTenantId(tenantId: String): Set<String>

    //endregion your codes 2

}