package io.kudos.ams.auth.core.service.iservice

import io.kudos.ams.auth.core.model.po.AuthGroupRole
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 组-角色关系业务接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface IAuthGroupRoleService : IBaseCrudService<String, AuthGroupRole> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据组ID获取角色ID集合
     *
     * @param groupId 组ID
     * @return 角色ID集合
     * @author AI: Codex
     * @since 1.0.0
     */
    fun getRoleIdsByGroupId(groupId: String): Set<String>

    /**
     * 根据角色ID获取组ID集合
     *
     * @param roleId 角色ID
     * @return 组ID集合
     * @author AI: Codex
     * @since 1.0.0
     */
    fun getGroupIdsByRoleId(roleId: String): Set<String>

    /**
     * 批量绑定组和角色关系
     *
     * @param groupId 组ID
     * @param roleIds 角色ID集合
     * @return 成功绑定的数量
     * @author AI: Codex
     * @since 1.0.0
     */
    fun batchBind(groupId: String, roleIds: Collection<String>): Int

    /**
     * 解绑组和角色关系
     *
     * @param groupId 组ID
     * @param roleId 角色ID
     * @return 是否解绑成功
     * @author AI: Codex
     * @since 1.0.0
     */
    fun unbind(groupId: String, roleId: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param groupId 组ID
     * @param roleId 角色ID
     * @return 是否存在
     * @author AI: Codex
     * @since 1.0.0
     */
    fun exists(groupId: String, roleId: String): Boolean

    //endregion your codes 2

}
