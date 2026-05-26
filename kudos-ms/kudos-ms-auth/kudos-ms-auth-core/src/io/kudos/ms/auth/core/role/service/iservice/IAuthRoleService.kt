package io.kudos.ms.auth.core.role.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.common.role.vo.response.AuthRoleRow
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry


/**
 * Role business interface
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IAuthRoleService : IBaseCrudService<String, AuthRole> {


    /**
     * Get the list of all user IDs that hold the given role
     *
     * @param roleId Role ID
     * @return List<String> List of user IDs; returns empty list if role does not exist or has no users
     */
    fun getRoleUserIds(roleId: String): List<String>

    /**
     * Get the list of all resource IDs owned by the given role
     *
     * @param roleId Role ID
     * @return Set<String> List of resource IDs; returns empty set if role does not exist or has no resources
     */
    fun getRoleResourceIds(roleId: String): Set<String>

    /**
     * Get the list of all active role IDs under the given tenant.
     * Only returns role IDs with active=true.
     *
     * @param tenantId Tenant ID
     * @return List<String> List of role IDs
     */
    fun getRoleIds(tenantId: String): List<String>

    /**
     * Get the list of all users who hold the given role
     *
     * @param roleId Role ID
     * @return List<UserAccountCacheEntry> List of users; returns empty list if role does not exist or has no users
     */
    fun getRoleUsers(roleId: String): List<UserAccountCacheEntry>

    /**
     * Get the list of all resources owned by the given role
     *
     * @param roleId Role ID
     * @return List<SysResourceCacheEntry> List of resources; returns empty list if role does not exist or has no resources
     */
    fun getRoleResources(roleId: String): List<SysResourceCacheEntry>

    /**
     * Check whether the role owns the specified resource
     *
     * @param roleId Role ID
     * @param resourceId Resource ID
     * @return true if the role owns the resource, false otherwise
     */
    fun hasResource(roleId: String, resourceId: String): Boolean

    /**
     * Get role information by tenant ID and role code
     *
     * @param tenantId Tenant ID
     * @param roleCode Role code
     * @return Role cache entry; returns null if not found
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleByTenantIdAndCode(tenantId: String, roleCode: String): AuthRoleCacheEntry?

    /**
     * Get role record by ID (non-cached)
     *
     * @param id Role ID
     * @return Role record; returns null if not found
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRoleRecord(id: String): AuthRoleRow?

    /**
     * Get role list by tenant ID
     *
     * @param tenantId Tenant ID
     * @return List of role records
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRolesByTenantId(tenantId: String): List<AuthRoleRow>

    /**
     * Get role list by subsystem code
     *
     * @param tenantId Tenant ID
     * @param subsysCode Subsystem code
     * @return List of role records
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getRolesBySubsysCode(tenantId: String, subsysCode: String): List<AuthRoleRow>

    /**
     * Update role active status
     *
     * @param id Role ID
     * @param active Whether enabled
     * @return Whether update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * Get the list of all role IDs owned by the given user
     *
     * @param userId User ID
     * @return List<String> List of role IDs; returns empty list if user does not exist or has no roles
     */
    fun getUserRoleIds(userId: String): List<String>

    /**
     * Check whether the user owns the specified role
     *
     * @param userId User ID
     * @param roleId Role ID
     * @return true if the user owns the role, false otherwise
     */
    fun hasRole(userId: String, roleId: String): Boolean

    /**
     * Check whether the user owns the role with the specified role code
     *
     * @param userId User ID
     * @param tenantId Tenant ID
     * @param roleCode Role code
     * @return true if the user owns the role, false otherwise
     */
    fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean

    /**
     * Get the list of all roles owned by the given user
     *
     * @param userId User ID
     * @return List<AuthRoleCacheEntry> List of roles; returns empty list if user does not exist or has no roles
     */
    fun getUserRoles(userId: String): List<AuthRoleCacheEntry>

    /**
     * Get user list by role code
     *
     * @param tenantId Tenant ID
     * @param roleCode Role code
     * @return List of user records
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUsersByRoleCode(tenantId: String, roleCode: String): List<UserAccountCacheEntry>

    /**
     * Check whether the user has access permission to the specified resource
     *
     * @param userId User ID
     * @param resourceId Resource ID
     * @return true if the user has access permission to the resource, false otherwise
     */
    fun isUserHasResource(userId: String, resourceId: String): Boolean

    /**
     * Get the list of all resource IDs owned by the given user
     *
     * @param userId User ID
     * @return Set<String> List of resource IDs; returns empty set if user does not exist or has no resources
     */
    fun getUserResourceIds(userId: String): Set<String>

    /**
     * Get the list of resource cache objects the given user has permission to access.
     * Query flow: user -> role -> resource (three-level association)
     *
     * @param userId User ID
     * @return List<SysResourceCacheEntry> List of resource cache objects; returns empty list if user does not exist or has no resources
     */
    fun getResources(userId: String): List<SysResourceCacheEntry>


}
