package io.kudos.ams.auth.common.api

import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem


/**
 * 部门 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthDeptApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据id从缓存中获取部门信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 部门id
     * @return AuthDeptCacheItem, 找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getDeptById(id: String): AuthDeptCacheItem?

    /**
     * 根据多个id从缓存中批量获取部门信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 部门id集合
     * @return Map<部门id，AuthDeptCacheItem>
     * @author K
     * @since 1.0.0
     */
    fun getDeptsByIds(ids: Collection<String>): Map<String, AuthDeptCacheItem>

    /**
     * 根据租户ID从缓存中获取其下所有部门ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     * 只返回active=true的部门ID
     *
     * @param tenantId 租户ID
     * @return List<部门ID>
     * @author K
     * @since 1.0.0
     */
    fun getDeptIds(tenantId: String): List<String>

    /**
     * 根据部门ID获取该部门的所有管理员用户信息
     *
     * @param deptId 部门ID
     * @return List<AuthUserCacheItem> 部门管理员用户列表，如果没有管理员则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getDeptAdmins(deptId: String): List<AuthUserCacheItem>

    /**
     * 根据部门ID获取该部门下的所有用户列表（包括管理员和普通用户）
     *
     * @param deptId 部门ID
     * @return List<AuthUserCacheItem> 用户列表，如果部门不存在或没有用户则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getDeptUsers(deptId: String): List<AuthUserCacheItem>

    /**
     * 检查用户是否属于指定部门
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return true表示用户属于该部门，false表示不属于
     * @author K
     * @since 1.0.0
     */
    fun isUserInDept(userId: String, deptId: String): Boolean

    /**
     * 根据部门ID获取该部门的所有直接子部门列表
     *
     * @param deptId 部门ID
     * @return List<AuthDeptCacheItem> 子部门列表，如果没有子部门则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getChildDepts(deptId: String): List<AuthDeptCacheItem>

    /**
     * 根据部门ID获取该部门的父部门
     *
     * @param deptId 部门ID
     * @return AuthDeptCacheItem 父部门，如果没有父部门则返回null
     * @author K
     * @since 1.0.0
     */
    fun getParentDept(deptId: String): AuthDeptCacheItem?

    //endregion your codes 2

}
