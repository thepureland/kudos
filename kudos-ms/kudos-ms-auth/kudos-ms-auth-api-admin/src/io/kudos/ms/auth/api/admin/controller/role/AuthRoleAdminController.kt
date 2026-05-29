package io.kudos.ms.auth.api.admin.controller.role

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.auth.common.role.vo.request.AuthRoleBatchBindUsersRequest
import io.kudos.ms.auth.common.role.vo.request.AuthRoleCopyRequest
import io.kudos.ms.auth.common.role.vo.request.AuthRoleFormCreate
import io.kudos.ms.auth.common.role.vo.request.AuthRoleFormUpdate
import io.kudos.ms.auth.common.role.vo.request.AuthRoleQuery
import io.kudos.ms.auth.common.role.vo.response.AuthRoleDetail
import io.kudos.ms.auth.common.role.vo.response.AuthRoleEdit
import io.kudos.ms.auth.common.role.vo.response.AuthRoleRow
import io.kudos.ms.auth.common.role.vo.response.BatchBindResultVo
import io.kudos.ms.auth.common.role.vo.response.EffectivePermissionsVo
import io.kudos.ms.auth.common.role.vo.response.RoleDeleteImpactVo
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleResourceService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * Role administration controller.
 *
 * In addition to standard CRUD, this proxies two relationship types: role-to-user and role-to-resource.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/role")
class AuthRoleAdminController :
    BaseCrudController<String, IAuthRoleService, AuthRoleQuery, AuthRoleRow, AuthRoleDetail, AuthRoleEdit, AuthRoleFormCreate, AuthRoleFormUpdate>() {

    @Resource
    private lateinit var authRoleUserService: IAuthRoleUserService

    @Resource
    private lateinit var authRoleResourceService: IAuthRoleResourceService

    /** Update the active state of a role. */
    @PutMapping("/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean =
        service.updateActive(id, active)

    /** List the user IDs belonging to a given role. */
    @GetMapping("/listUserIds")
    fun listUserIds(@RequestParam roleId: String): Set<String> =
        authRoleUserService.getUserIdsByRoleId(roleId)

    /** List the role IDs held by a given user. */
    @GetMapping("/listRoleIdsByUser")
    fun listRoleIdsByUser(@RequestParam userId: String): Set<String> =
        authRoleUserService.getRoleIdsByUserId(userId)

    /** Batch-assign users to a role. Returns the number of newly created relations. */
    @PostMapping("/bindUsers")
    fun bindUsers(@RequestParam roleId: String, @RequestBody userIds: Collection<String>): Int =
        authRoleUserService.batchBind(roleId, userIds)

    /** Unbind a role-to-user relation. */
    @DeleteMapping("/unbindUser")
    fun unbindUser(@RequestParam roleId: String, @RequestParam userId: String): Boolean =
        authRoleUserService.unbind(roleId, userId)

    /** List the resource IDs granted to a given role. */
    @GetMapping("/listResourceIds")
    fun listResourceIds(@RequestParam roleId: String): Set<String> =
        authRoleResourceService.getResourceIdsByRoleId(roleId)

    /** List the role IDs that reference a given resource. */
    @GetMapping("/listRoleIdsByResource")
    fun listRoleIdsByResource(@RequestParam resourceId: String): Set<String> =
        authRoleResourceService.getRoleIdsByResourceId(resourceId)

    /** Batch-grant resources to a role. Returns the number of newly created relations. */
    @PostMapping("/bindResources")
    fun bindResources(@RequestParam roleId: String, @RequestBody resourceIds: Collection<String>): Int =
        authRoleResourceService.batchBind(roleId, resourceIds)

    /** Unbind a role-to-resource relation. */
    @DeleteMapping("/unbindResource")
    fun unbindResource(@RequestParam roleId: String, @RequestParam resourceId: String): Boolean =
        authRoleResourceService.unbind(roleId, resourceId)

    // -- Aggregators ------------------------------------------------------------
    //
    // Each of the four endpoints below replaces an N+M+K fan-out the console UI was making
    // pre-aggregator. They are HTTP-thin: the heavy lifting (transaction boundaries, cache
    // composition, partial-failure aggregation) lives in IAuthRoleService.

    /**
     * One-shot snapshot of a user's effective permissions (direct roles + groups + inherited
     * roles + resources). Replaces the AccountEffectivePermissionsDialog's 1 + 1 + N + M + 3
     * fan-out with a single round trip.
     */
    @GetMapping("/getEffectivePermissions")
    fun getEffectivePermissions(@RequestParam userId: String): EffectivePermissionsVo =
        service.getEffectivePermissions(userId)

    /**
     * Pre-delete impact summary across a batch of roles: distinct counts of users and groups
     * currently bound to any of the supplied [roleIds]. Body is a JSON array of role ids.
     */
    @PostMapping("/getDeleteImpact")
    fun getDeleteImpact(@RequestBody roleIds: List<String>): RoleDeleteImpactVo =
        service.getDeleteImpact(roleIds)

    /**
     * Cartesian-product batch-bind: every user in `userIds` is bound to every role in `roleIds`.
     * Per-role transactional boundary — partial failures are returned in the response payload
     * rather than rolled back.
     */
    @PostMapping("/batchBindUsers")
    fun batchBindUsers(@RequestBody request: AuthRoleBatchBindUsersRequest): BatchBindResultVo =
        service.batchBindUsers(request.roleIds, request.userIds)

    /**
     * Atomic role-copy: read source, save new role (overriding `code`/`name`), optionally copy
     * resource grants — all in one transaction. Returns the new role id.
     */
    @PostMapping("/copyRole")
    fun copyRole(@RequestBody request: AuthRoleCopyRequest): String =
        service.copyRole(request.sourceId, request.code, request.name, request.copyResources)

}
