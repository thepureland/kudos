package io.kudos.ams.auth.provider.service.iservice

import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.auth.provider.model.po.AuthDept


/**
 * 部门业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthDeptService : IBaseCrudService<String, AuthDept> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据部门ID获取该部门的所有管理员用户信息
     *
     * @param deptId 部门ID
     * @return List<AuthUserCacheItem> 部门管理员用户列表，如果没有管理员则返回空列表
     */
    fun getDeptAdmins(deptId: String): List<AuthUserCacheItem>

    /**
     * 根据部门ID获取该部门下的所有用户ID列表（包括管理员和普通用户）
     *
     * @param deptId 部门ID
     * @return List<String> 用户ID列表，如果部门不存在或没有用户则返回空列表
     */
    fun getDeptUserIds(deptId: String): List<String>

    /**
     * 根据部门ID获取该部门的所有直接子部门ID列表
     *
     * @param deptId 部门ID
     * @return List<String> 子部门ID列表，如果没有子部门则返回空列表
     */
    fun getChildDeptIds(deptId: String): List<String>

    /**
     * 根据部门ID获取该部门下的所有用户列表（包括管理员和普通用户）
     *
     * @param deptId 部门ID
     * @return List<AuthUserCacheItem> 用户列表，如果部门不存在或没有用户则返回空列表
     */
    fun getDeptUsers(deptId: String): List<AuthUserCacheItem>

    /**
     * 检查用户是否属于指定部门
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return true表示用户属于该部门，false表示不属于
     */
    fun isUserInDept(userId: String, deptId: String): Boolean

    /**
     * 根据部门ID获取该部门的所有直接子部门列表
     *
     * @param deptId 部门ID
     * @return List<AuthDeptCacheItem> 子部门列表，如果没有子部门则返回空列表
     */
    fun getChildDepts(deptId: String): List<AuthDeptCacheItem>

    /**
     * 根据部门ID获取该部门的父部门
     *
     * @param deptId 部门ID
     * @return AuthDeptCacheItem 父部门，如果没有父部门则返回null
     */
    fun getParentDept(deptId: String): AuthDeptCacheItem?

    //endregion your codes 2

}
