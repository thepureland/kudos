package io.kudos.ams.auth.provider.service.iservice

import io.kudos.ams.auth.provider.model.po.AuthRoleResource
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 角色-资源关系业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthRoleResourceService : IBaseCrudService<String, AuthRoleResource> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据角色ID获取资源ID集合
     *
     * @param roleId 角色ID
     * @return 资源ID集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getResourceIdsByRoleId(roleId: String): Set<String>

    /**
     * 根据资源ID获取角色ID集合
     *
     * @param resourceId 资源ID
     * @return 角色ID集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleIdsByResourceId(resourceId: String): Set<String>

    /**
     * 批量绑定角色和资源关系
     *
     * @param roleId 角色ID
     * @param resourceIds 资源ID集合
     * @return 成功绑定的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(roleId: String, resourceIds: Collection<String>): Int

    /**
     * 解绑角色和资源关系
     *
     * @param roleId 角色ID
     * @param resourceId 资源ID
     * @return 是否解绑成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(roleId: String, resourceId: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param roleId 角色ID
     * @param resourceId 资源ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(roleId: String, resourceId: String): Boolean

    //endregion your codes 2

}
