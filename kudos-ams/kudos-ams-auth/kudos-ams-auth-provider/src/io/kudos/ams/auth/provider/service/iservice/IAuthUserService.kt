package io.kudos.ams.auth.provider.service.iservice

import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.auth.provider.model.po.AuthUser


/**
 * 用户业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthUserService : IBaseCrudService<String, AuthUser> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据用户ID获取该用户有权限访问的资源缓存对象列表
     * 查询流程：用户 → 角色 → 资源（三级关联）
     *
     * @param userId 用户ID
     * @return List<SysResourceCacheItem> 资源缓存对象列表，如果用户不存在或没有资源则返回空列表
     */
    fun getResources(userId: String): List<SysResourceCacheItem>

    /**
     * 根据用户ID获取该用户拥有的所有角色ID列表
     *
     * @param userId 用户ID
     * @return List<String> 角色ID列表，如果用户不存在或没有角色则返回空列表
     */
    fun getUserRoleIds(userId: String): List<String>

    /**
     * 根据用户ID获取该用户所属的所有部门ID列表
     *
     * @param userId 用户ID
     * @return List<String> 部门ID列表，如果用户不存在或没有部门则返回空列表
     */
    fun getUserDeptIds(userId: String): List<String>

    /**
     * 根据用户ID获取该用户拥有的所有资源ID列表
     *
     * @param userId 用户ID
     * @return List<String> 资源ID列表，如果用户不存在或没有资源则返回空列表
     */
    fun getUserResourceIds(userId: String): List<String>

    /**
     * 根据租户ID获取该租户下所有激活用户的ID列表
     * 只返回active=true的用户ID
     *
     * @param tenantId 租户ID
     * @return List<String> 用户ID列表
     */
    fun getUserIds(tenantId: String): List<String>

    /**
     * 根据用户ID获取该用户拥有的所有角色列表
     *
     * @param userId 用户ID
     * @return List<AuthRoleCacheItem> 角色列表，如果用户不存在或没有角色则返回空列表
     */
    fun getUserRoles(userId: String): List<AuthRoleCacheItem>

    /**
     * 根据用户ID获取该用户所属的所有部门列表
     *
     * @param userId 用户ID
     * @return List<AuthDeptCacheItem> 部门列表，如果用户不存在或没有部门则返回空列表
     */
    fun getUserDepts(userId: String): List<AuthDeptCacheItem>

    /**
     * 检查用户是否拥有指定角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return true表示用户拥有该角色，false表示不拥有
     */
    fun hasRole(userId: String, roleId: String): Boolean

    /**
     * 检查用户是否拥有指定角色编码的角色
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @return true表示用户拥有该角色，false表示不拥有
     */
    fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean

    /**
     * 检查用户是否属于指定部门
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return true表示用户属于该部门，false表示不属于
     */
    fun isUserInDept(userId: String, deptId: String): Boolean

    /**
     * 检查用户是否有指定资源的访问权限
     *
     * @param userId 用户ID
     * @param resourceId 资源ID
     * @return true表示用户有该资源的访问权限，false表示没有
     */
    fun hasResource(userId: String, resourceId: String): Boolean

    //endregion your codes 2

}
