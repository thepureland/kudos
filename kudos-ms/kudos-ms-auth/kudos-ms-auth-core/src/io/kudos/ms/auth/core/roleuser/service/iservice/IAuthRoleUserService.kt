package io.kudos.ms.auth.core.roleuser.service.iservice
import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.core.roleuser.model.po.AuthRoleUser


/**
 * 角色-用户关系业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IAuthRoleUserService : IBaseCrudService<String, AuthRoleUser> {


    /**
     * 根据角色ID获取用户ID集合
     *
     * @param roleId 角色ID
     * @return 用户ID集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserIdsByRoleId(roleId: String): Set<String>

    /**
     * 根据用户ID获取角色ID集合
     *
     * @param userId 用户ID
     * @return 角色ID集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleIdsByUserId(userId: String): Set<String>

    /**
     * 批量绑定角色和用户关系
     *
     * @param roleId 角色ID
     * @param userIds 用户ID集合
     * @return 成功绑定的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(roleId: String, userIds: Collection<String>): Int

    /**
     * 解绑角色和用户关系
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 是否解绑成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(roleId: String, userId: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(roleId: String, userId: String): Boolean


}
