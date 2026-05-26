package io.kudos.ms.auth.api.admin.controller.role

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.auth.common.role.vo.request.AuthRoleFormCreate
import io.kudos.ms.auth.common.role.vo.request.AuthRoleFormUpdate
import io.kudos.ms.auth.common.role.vo.request.AuthRoleQuery
import io.kudos.ms.auth.common.role.vo.response.AuthRoleDetail
import io.kudos.ms.auth.common.role.vo.response.AuthRoleEdit
import io.kudos.ms.auth.common.role.vo.response.AuthRoleRow
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

}
