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
 * 用户组管理控制器。
 *
 * 在标准 CRUD 之外，还代理两类关系：组↔用户、组↔角色。关系 CRUD 不走 BaseCrudController，
 * 因为以"业务批量绑/解"为主，不需要按主键单条增删的语义。
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

    /** 列出指定组下的用户 ID */
    @GetMapping("/listUserIds")
    fun listUserIds(@RequestParam groupId: String): Set<String> =
        authGroupUserService.getUserIdsByGroupId(groupId)

    /** 列出指定用户加入的组 ID */
    @GetMapping("/listGroupIdsByUser")
    fun listGroupIdsByUser(@RequestParam userId: String): Set<String> =
        authGroupUserService.getGroupIdsByUserId(userId)

    /** 批量为某组添加用户。返回新增的关系数。 */
    @PostMapping("/bindUsers")
    fun bindUsers(@RequestParam groupId: String, @RequestBody userIds: Collection<String>): Int =
        authGroupUserService.batchBind(groupId, userIds)

    /** 解绑组↔用户关系。 */
    @DeleteMapping("/unbindUser")
    fun unbindUser(@RequestParam groupId: String, @RequestParam userId: String): Boolean =
        authGroupUserService.unbind(groupId, userId)

    /** 列出指定组下的角色 ID */
    @GetMapping("/listRoleIds")
    fun listRoleIds(@RequestParam groupId: String): Set<String> =
        authGroupRoleService.getRoleIdsByGroupId(groupId)

    /** 列出拥有指定角色的组 ID */
    @GetMapping("/listGroupIdsByRole")
    fun listGroupIdsByRole(@RequestParam roleId: String): Set<String> =
        authGroupRoleService.getGroupIdsByRoleId(roleId)

    /** 批量为某组绑定角色。返回新增的关系数。 */
    @PostMapping("/bindRoles")
    fun bindRoles(@RequestParam groupId: String, @RequestBody roleIds: Collection<String>): Int =
        authGroupRoleService.batchBind(groupId, roleIds)

    /** 解绑组↔角色关系。 */
    @DeleteMapping("/unbindRole")
    fun unbindRole(@RequestParam groupId: String, @RequestParam roleId: String): Boolean =
        authGroupRoleService.unbind(groupId, roleId)

}
