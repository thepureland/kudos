package io.kudos.ms.auth.api.admin.controller.role

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.auth.common.delegation.vo.RevokeImpactVo
import io.kudos.ms.auth.common.delegation.vo.RoleGrantRequest
import io.kudos.ms.auth.common.role.vo.PermissionBindingRequest
import io.kudos.ms.auth.common.role.vo.PermissionBindingVo
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
import io.kudos.ms.user.common.passport.CurrentUserKit
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
 * @author AI: Claude
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
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "启用/停用角色")
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
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "角色绑定用户")
    @PostMapping("/bindUsers")
    fun bindUsers(@RequestParam roleId: String, @RequestBody userIds: Collection<String>): Int =
        authRoleUserService.batchBind(roleId, userIds)

    /** Unbind a role-to-user relation. */
    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "角色解绑用户")
    @DeleteMapping("/unbindUser")
    fun unbindUser(@RequestParam roleId: String, @RequestParam userId: String): Boolean =
        authRoleUserService.unbind(roleId, userId)

    // -- Delegation ------------------------------------------------------------
    //
    // Separate from bindUsers on purpose: a delegation is somebody passing on authority they were
    // given, so it is screened against what they hold and can be withdrawn when they lose it.

    /**
     * Hand a role to principals as a delegated grant, on behalf of the caller. Returns the new
     * grant ids.
     */
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "转授角色")
    @PostMapping("/grant")
    fun grant(@RequestBody request: RoleGrantRequest): List<String> =
        authRoleUserService.grant(request, CurrentUserKit.currentUserId())

    /**
     * What revoking a grant would take away, including everything delegated onwards from it. Always
     * show this before calling `/revoke` — the blast radius is invisible from the grant itself.
     */
    @GetMapping("/getRevokeImpact")
    fun getRevokeImpact(@RequestParam grantId: String): RevokeImpactVo =
        authRoleUserService.getRevokeImpact(grantId)

    /** Revoke a grant and, transitively, everything delegated from it. Returns the number revoked. */
    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "回收授权（级联）")
    @DeleteMapping("/revoke")
    fun revoke(@RequestParam grantId: String, @RequestParam(required = false) reason: String?): Int =
        authRoleUserService.revoke(grantId, reason)

    /** List the resource IDs granted to a given role. */
    @GetMapping("/listResourceIds")
    fun listResourceIds(@RequestParam roleId: String): Set<String> =
        authRoleResourceService.getResourceIdsByRoleId(roleId)

    /** List the role IDs that reference a given resource. */
    @GetMapping("/listRoleIdsByResource")
    fun listRoleIdsByResource(@RequestParam resourceId: String): Set<String> =
        authRoleResourceService.getRoleIdsByResourceId(resourceId)

    /** Batch-grant resources to a role. Returns the number of newly created relations. */
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "角色绑定权限")
    @PostMapping("/bindResources")
    fun bindResources(@RequestParam roleId: String, @RequestBody resourceIds: Collection<String>): Int =
        authRoleResourceService.batchBind(roleId, resourceIds)

    // -- Code-addressed permission bindings ------------------------------------
    //
    // Separate endpoints from bindResources: a resource binding is a node in a tree, while these
    // carry an effect and a condition of their own.

    /** The role's code-addressed bindings (`sys:user:*`, ALLOW/DENY, optional condition). */
    @GetMapping("/listPermissionBindings")
    fun listPermissionBindings(@RequestParam roleId: String): List<PermissionBindingVo> =
        authRoleResourceService.listPermissionBindings(roleId)

    /** Create or update one code-addressed binding. Returns its id. */
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "保存权限编码绑定")
    @PostMapping("/savePermissionBinding")
    fun savePermissionBinding(@RequestBody request: PermissionBindingRequest): String =
        authRoleResourceService.savePermissionBinding(request)

    /** Remove one code-addressed binding. */
    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "删除权限编码绑定")
    @DeleteMapping("/removePermissionBinding")
    fun removePermissionBinding(@RequestParam bindingId: String): Boolean =
        authRoleResourceService.removePermissionBinding(bindingId)

    /** Unbind a role-to-resource relation. */
    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "角色解绑权限")
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
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "批量绑定角色用户")
    @PostMapping("/batchBindUsers")
    fun batchBindUsers(@RequestBody request: AuthRoleBatchBindUsersRequest): BatchBindResultVo =
        service.batchBindUsers(request.roleIds, request.userIds)

    /**
     * Atomic role-copy: read source, save new role (overriding `code`/`name`), optionally copy
     * resource grants — all in one transaction. Returns the new role id.
     */
    @PostMapping("/copyRole")
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "复制角色")
    fun copyRole(@RequestBody request: AuthRoleCopyRequest): String =
        service.copyRole(request.sourceId, request.code, request.name, request.copyResources)

    companion object {
        private const val MODULE_CODE = "auth-role"
    }
}
