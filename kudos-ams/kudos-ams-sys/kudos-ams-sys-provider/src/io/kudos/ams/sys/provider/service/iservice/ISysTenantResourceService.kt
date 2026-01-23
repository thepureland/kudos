package io.kudos.ams.sys.provider.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.provider.model.po.SysTenantResource


/**
 * 租户-资源关系业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysTenantResourceService : IBaseCrudService<String, SysTenantResource> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据租户id获取资源id列表
     *
     * @param tenantId 租户id
     * @return 资源id集合
     * @author K
     * @since 1.0.0
     */
    fun getResourceIdsByTenantId(tenantId: String): Set<String>

    /**
     * 根据资源id获取租户id列表
     *
     * @param resourceId 资源id
     * @return 租户id集合
     * @author K
     * @since 1.0.0
     */
    fun getTenantIdsByResourceId(resourceId: String): Set<String>

    /**
     * 批量绑定租户与资源的关系
     *
     * @param tenantId 租户id
     * @param resourceIds 资源id集合
     * @return 成功绑定的数量
     * @author K
     * @since 1.0.0
     */
    fun batchBind(tenantId: String, resourceIds: Collection<String>): Int

    /**
     * 解绑租户与资源的关系
     *
     * @param tenantId 租户id
     * @param resourceId 资源id
     * @return 是否解绑成功
     * @author K
     * @since 1.0.0
     */
    fun unbind(tenantId: String, resourceId: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param resourceId 资源id
     * @return 是否存在
     * @author K
     * @since 1.0.0
     */
    fun exists(tenantId: String, resourceId: String): Boolean

    //endregion your codes 2

}