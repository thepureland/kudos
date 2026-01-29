package io.kudos.ams.user.provider.service.iservice

import io.kudos.ams.user.provider.model.po.AuthDeptUser
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 部门-用户关系业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthDeptUserService : IBaseCrudService<String, AuthDeptUser> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据部门ID获取用户ID集合
     *
     * @param deptId 部门ID
     * @return 用户ID集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserIdsByDeptId(deptId: String): Set<String>

    /**
     * 根据用户ID获取部门ID集合
     *
     * @param userId 用户ID
     * @return 部门ID集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDeptIdsByUserId(userId: String): Set<String>

    /**
     * 批量绑定部门和用户关系
     *
     * @param deptId 部门ID
     * @param userIds 用户ID集合
     * @param deptAdmin 是否为部门管理员，默认为false
     * @return 成功绑定的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(deptId: String, userIds: Collection<String>, deptAdmin: Boolean = false): Int

    /**
     * 解绑部门和用户关系
     *
     * @param deptId 部门ID
     * @param userId 用户ID
     * @return 是否解绑成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(deptId: String, userId: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param deptId 部门ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(deptId: String, userId: String): Boolean

    /**
     * 设置/取消部门管理员
     *
     * @param deptId 部门ID
     * @param userId 用户ID
     * @param isAdmin 是否为管理员
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun setDeptAdmin(deptId: String, userId: String, isAdmin: Boolean): Boolean

    //endregion your codes 2

}
