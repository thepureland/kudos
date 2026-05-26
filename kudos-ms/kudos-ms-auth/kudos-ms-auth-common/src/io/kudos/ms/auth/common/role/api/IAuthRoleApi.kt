package io.kudos.ms.auth.common.role.api

import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * Role public API.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IAuthRoleApi {


    /**
     * Retrieves role info from the cache by id; if absent, loads from the database and writes back to the cache.
     *
     * @param id Role id.
     * @return AuthRoleCacheEntry, or null if not found.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/getRoleById")
    fun getRoleById(@RequestParam id: String): AuthRoleCacheEntry?

    /**
     * Batch-retrieves role info from the cache by multiple ids; entries missing from the cache are loaded from the
     * database and written back to the cache.
     *
     * @param ids Collection of role ids.
     * @return Map of role id to AuthRoleCacheEntry.
     * @author K
     * @since 1.0.0
     */
    @PostMapping("/api/internal/auth/role/getRolesByIds")
    fun getRolesByIds(@RequestBody ids: Collection<String>): Map<String, AuthRoleCacheEntry>

    /**
     * Retrieves the role id from the cache by tenant id and role code; if absent, loads from the database and writes
     * back to the cache. Only returns the id of roles with active = true.
     *
     * @param tenantId Tenant id.
     * @param code Role code.
     * @return Role id, or null if not found.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/getRoleId")
    fun getRoleId(@RequestParam tenantId: String, @RequestParam code: String): String?

    /**
     * Retrieves all users that hold the given role id.
     *
     * @param roleId Role id.
     * @return List of UserAccountCacheEntry; empty list if the role does not exist or has no users.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/getRoleUsers")
    fun getRoleUsers(@RequestParam roleId: String): List<UserAccountCacheEntry>

    /**
     * Retrieves all user ids that hold the role identified by tenant id and role code.
     *
     * @param tenantId Tenant id.
     * @param roleCode Role code.
     * @return List of user ids; empty list if the role does not exist or has no users.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/getUserIdsByRoleCode")
    fun getUserIdsByRoleCode(@RequestParam tenantId: String, @RequestParam roleCode: String): List<String>

    /**
     * Retrieves all resources held by the given role id.
     *
     * @param roleId Role id.
     * @return List of SysResourceCacheEntry; empty list if the role does not exist or has no resources.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/getRoleResources")
    fun getRoleResources(@RequestParam roleId: String): List<SysResourceCacheEntry>

    /**
     * Checks whether a role holds the given resource.
     *
     * @param roleId Role id.
     * @param resourceId Resource id.
     * @return true if the role holds the resource, false otherwise.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/hasResource")
    fun hasResource(@RequestParam roleId: String, @RequestParam resourceId: String): Boolean

    /**
     * Retrieves the ids of all active roles under the given tenant.
     * Only returns ids of roles with active = true.
     *
     * @param tenantId Tenant id.
     * @return List of role ids.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/getRoleIds")
    fun getRoleIds(@RequestParam tenantId: String): List<String>

    /**
     * Retrieves the resource cache entries the given user is permitted to access.
     * Lookup path: user -> role -> resource (three-level join).
     *
     * @param userId User id.
     * @return List of SysResourceCacheEntry; empty list if the user does not exist or has no resources.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/getResources")
    fun getResources(@RequestParam userId: String): List<SysResourceCacheEntry>

    /**
     * Retrieves all roles held by the given user.
     *
     * @param userId User id.
     * @return List of AuthRoleCacheEntry; empty list if the user does not exist or has no roles.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/getUserRoles")
    fun getUserRoles(@RequestParam userId: String): List<AuthRoleCacheEntry>

    /**
     * Checks whether the user holds the given role.
     *
     * @param userId User id.
     * @param roleId Role id.
     * @return true if the user holds the role, false otherwise.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/hasRole")
    fun hasRole(@RequestParam userId: String, @RequestParam roleId: String): Boolean

    /**
     * Checks whether the user holds the role identified by the given role code.
     *
     * @param userId User id.
     * @param tenantId Tenant id.
     * @param roleCode Role code.
     * @return true if the user holds the role, false otherwise.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/hasRoleByCode")
    fun hasRoleByCode(
        @RequestParam userId: String,
        @RequestParam tenantId: String,
        @RequestParam roleCode: String,
    ): Boolean

    /**
     * Checks whether the user is permitted to access the given resource.
     *
     * @param userId User id.
     * @param resourceId Resource id.
     * @return true if the user is permitted to access the resource, false otherwise.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/auth/role/isUserHasResource")
    fun isUserHasResource(@RequestParam userId: String, @RequestParam resourceId: String): Boolean



}
