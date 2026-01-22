package io.kudos.ams.auth.common.api

import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem


/**
 * 用户 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthUserApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据id从缓存中获取用户信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 用户id
     * @return AuthUserCacheItem, 找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getUserById(id: String): AuthUserCacheItem?

    /**
     * 根据多个id从缓存中批量获取用户信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 用户id集合
     * @return Map<用户id，AuthUserCacheItem>
     * @author K
     * @since 1.0.0
     */
    fun getUsersByIds(ids: Collection<String>): Map<String, AuthUserCacheItem>

    /**
     * 根据租户ID和用户名从缓存获取对应的用户ID，如果缓存中不存在，则从数据库中加载，并写回缓存
     * 只返回active=true的用户ID
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 用户ID，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getUserId(tenantId: String, username: String): String?

    /**
     * 根据用户ID获取该用户有权限访问的资源缓存对象列表
     * 查询流程：用户 → 角色 → 资源（三级关联）
     *
     * @param userId 用户ID
     * @return List<SysResourceCacheItem> 资源缓存对象列表，如果用户不存在或没有资源则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getResources(userId: String): List<SysResourceCacheItem>

    /**
     * 根据用户ID获取该用户拥有的所有角色列表
     *
     * @param userId 用户ID
     * @return List<AuthRoleCacheItem> 角色列表，如果用户不存在或没有角色则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getUserRoles(userId: String): List<AuthRoleCacheItem>

    /**
     * 根据用户ID获取该用户所属的所有部门列表
     *
     * @param userId 用户ID
     * @return List<AuthDeptCacheItem> 部门列表，如果用户不存在或没有部门则返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getUserDepts(userId: String): List<AuthDeptCacheItem>

    /**
     * 检查用户是否拥有指定角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return true表示用户拥有该角色，false表示不拥有
     * @author K
     * @since 1.0.0
     */
    fun hasRole(userId: String, roleId: String): Boolean

    /**
     * 检查用户是否拥有指定角色编码的角色
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @return true表示用户拥有该角色，false表示不拥有
     * @author K
     * @since 1.0.0
     */
    fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean

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
     * 检查用户是否有指定资源的访问权限
     *
     * @param userId 用户ID
     * @param resourceId 资源ID
     * @return true表示用户有该资源的访问权限，false表示没有
     * @author K
     * @since 1.0.0
     */
    fun hasResource(userId: String, resourceId: String): Boolean

    /**
     * 根据租户ID获取该租户下所有激活用户的ID列表
     * 只返回active=true的用户ID
     *
     * @param tenantId 租户ID
     * @return List<String> 用户ID列表
     * @author K
     * @since 1.0.0
     */
    fun getUserIds(tenantId: String): List<String>

    //endregion your codes 2

}
