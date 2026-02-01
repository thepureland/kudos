package io.kudos.ams.auth.core.service.iservice

import io.kudos.ams.auth.common.vo.role.AuthRoleCacheItem
import io.kudos.ams.auth.common.vo.role.AuthRoleRecord
import io.kudos.ams.auth.core.model.po.AuthRole
import io.kudos.ams.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ams.user.common.vo.user.UserAccountCacheItem
import io.kudos.ams.user.common.vo.user.UserAccountRecord
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 角色业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthRoleService : IBaseCrudService<String, AuthRole> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据角色ID获取拥有该角色的所有用户ID列表
     *
     * @param roleId 角色ID
     * @return List<String> 用户ID列表，如果角色不存在或没有用户则返回空列表
     */
    fun getRoleUserIds(roleId: String): List<String>

    /**
     * 根据角色ID获取该角色拥有的所有资源ID列表
     *
     * @param roleId 角色ID
     * @return List<String> 资源ID列表，如果角色不存在或没有资源则返回空列表
     */
    fun getRoleResourceIds(roleId: String): List<String>

    /**
     * 根据租户ID获取该租户下所有激活角色的ID列表
     * 只返回active=true的角色ID
     *
     * @param tenantId 租户ID
     * @return List<String> 角色ID列表
     */
    fun getRoleIds(tenantId: String): List<String>

    /**
     * 根据角色ID获取拥有该角色的所有用户列表
     *
     * @param roleId 角色ID
     * @return List<UserAccountCacheItem> 用户列表，如果角色不存在或没有用户则返回空列表
     */
    fun getRoleUsers(roleId: String): List<UserAccountCacheItem>

    /**
     * 根据角色ID获取该角色拥有的所有资源列表
     *
     * @param roleId 角色ID
     * @return List<SysResourceCacheItem> 资源列表，如果角色不存在或没有资源则返回空列表
     */
    fun getRoleResources(roleId: String): List<SysResourceCacheItem>

    /**
     * 检查角色是否拥有指定资源
     *
     * @param roleId 角色ID
     * @param resourceId 资源ID
     * @return true表示角色拥有该资源，false表示不拥有
     */
    fun hasResource(roleId: String, resourceId: String): Boolean

    /**
     * 根据租户ID和角色编码获取角色信息
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @return 角色缓存项，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleByTenantIdAndCode(tenantId: String, roleCode: String): AuthRoleCacheItem?

    /**
     * 根据ID获取角色记录（非缓存）
     *
     * @param id 角色ID
     * @return 角色记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleRecord(id: String): AuthRoleRecord?

    /**
     * 根据租户ID获取角色列表
     *
     * @param tenantId 租户ID
     * @return 角色记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRolesByTenantId(tenantId: String): List<AuthRoleRecord>

    /**
     * 根据子系统编码获取角色列表
     *
     * @param tenantId 租户ID
     * @param subsysCode 子系统编码
     * @return 角色记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRolesBySubsysCode(tenantId: String, subsysCode: String): List<AuthRoleRecord>

    /**
     * 更新角色启用状态
     *
     * @param id 角色ID
     * @param active 是否启用
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 根据用户ID获取该用户拥有的所有角色ID列表
     *
     * @param userId 用户ID
     * @return List<String> 角色ID列表，如果用户不存在或没有角色则返回空列表
     */
    fun getUserRoleIds(userId: String): List<String>

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
     * 根据用户ID获取该用户拥有的所有角色列表
     *
     * @param userId 用户ID
     * @return List<AuthRoleCacheItem> 角色列表，如果用户不存在或没有角色则返回空列表
     */
    fun getUserRoles(userId: String): List<AuthRoleCacheItem>

    /**
     * 根据角色编码获取用户列表
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @return 用户记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUsersByRoleCode(tenantId: String, roleCode: String): List<UserAccountCacheItem>

    /**
     * 检查用户是否有指定资源的访问权限
     *
     * @param userId 用户ID
     * @param resourceId 资源ID
     * @return true表示用户有该资源的访问权限，false表示没有
     */
    fun isUserHasResource(userId: String, resourceId: String): Boolean

    /**
     * 根据用户ID获取该用户拥有的所有资源ID列表
     *
     * @param userId 用户ID
     * @return List<String> 资源ID列表，如果用户不存在或没有资源则返回空列表
     */
    fun getUserResourceIds(userId: String): List<String>

    /**
     * 根据用户ID获取该用户有权限访问的资源缓存对象列表
     * 查询流程：用户 → 角色 → 资源（三级关联）
     *
     * @param userId 用户ID
     * @return List<SysResourceCacheItem> 资源缓存对象列表，如果用户不存在或没有资源则返回空列表
     */
    fun getResources(userId: String): List<SysResourceCacheItem>

    //endregion your codes 2

}
