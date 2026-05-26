package io.kudos.ms.auth.api.admin.controller.group

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.auth.common.group.vo.request.AuthGroupFormCreate
import io.kudos.ms.auth.common.group.vo.request.AuthGroupFormUpdate
import io.kudos.ms.auth.common.group.vo.request.AuthGroupQuery
import io.kudos.ms.auth.common.group.vo.response.AuthGroupDetail
import io.kudos.ms.auth.common.group.vo.response.AuthGroupEdit
import io.kudos.ms.auth.common.group.vo.response.AuthGroupRow
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * User group administration controller.
 *
 * In addition to standard CRUD, this proxies two relationship types: group-to-user and group-to-role.
 * Relation CRUD does not flow through BaseCrudController because the operations are batch bind/unbind
 * by business key, not single-record mutations keyed by primary id.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/group")
class AuthGroupAdminController :
    BaseCrudController<String, IAuthGroupService, AuthGroupQuery, AuthGroupRow, AuthGroupDetail, AuthGroupEdit, AuthGroupFormCreate, AuthGroupFormUpdate>() {

    @Resource
    private lateinit var authGroupUserService: IAuthGroupUserService

    @Resource
    private lateinit var authGroupRoleService: IAuthGroupRoleService

    /** List the user IDs belonging to a given group. */
    @GetMapping("/listUserIds")
    fun listUserIds(@RequestParam groupId: String): Set<String> =
        authGroupUserService.getUserIdsByGroupId(groupId)

    /** List the group IDs that a given user has joined. */
    @GetMapping("/listGroupIdsByUser")
    fun listGroupIdsByUser(@RequestParam userId: String): Set<String> =
        authGroupUserService.getGroupIdsByUserId(userId)

    /** Batch-add users to a group. Returns the number of newly created relations. */
    @PostMapping("/bindUsers")
    fun bindUsers(@RequestParam groupId: String, @RequestBody userIds: Collection<String>): Int =
        authGroupUserService.batchBind(groupId, userIds)

    /** Unbind a group-to-user relation. */
    @DeleteMapping("/unbindUser")
    fun unbindUser(@RequestParam groupId: String, @RequestParam userId: String): Boolean =
        authGroupUserService.unbind(groupId, userId)

    /** List the role IDs granted to a given group. */
    @GetMapping("/listRoleIds")
    fun listRoleIds(@RequestParam groupId: String): Set<String> =
        authGroupRoleService.getRoleIdsByGroupId(groupId)

    /** List the group IDs that hold a given role. */
    @GetMapping("/listGroupIdsByRole")
    fun listGroupIdsByRole(@RequestParam roleId: String): Set<String> =
        authGroupRoleService.getGroupIdsByRoleId(roleId)

    /** Batch-bind roles to a group. Returns the number of newly created relations. */
    @PostMapping("/bindRoles")
    fun bindRoles(@RequestParam groupId: String, @RequestBody roleIds: Collection<String>): Int =
        authGroupRoleService.batchBind(groupId, roleIds)

    /** Unbind a group-to-role relation. */
    @DeleteMapping("/unbindRole")
    fun unbindRole(@RequestParam groupId: String, @RequestParam roleId: String): Boolean =
        authGroupRoleService.unbind(groupId, roleId)

}
