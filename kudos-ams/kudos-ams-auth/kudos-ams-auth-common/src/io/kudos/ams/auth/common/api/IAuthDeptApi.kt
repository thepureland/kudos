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

    //endregion your codes 2

}
