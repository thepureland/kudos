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
 * 角色管理控制器。
 *
 * 在标准 CRUD 之外，还代理两类关系：角色↔用户、角色↔资源。
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

    /** 更新角色启用状态 */
    @PutMapping("/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean =
        service.updateActive(id, active)

    /** 列出指定角色下的用户 ID */
    @GetMapping("/listUserIds")
    fun listUserIds(@RequestParam roleId: String): Set<String> =
        authRoleUserService.getUserIdsByRoleId(roleId)

    /** 列出指定用户拥有的角色 ID */
    @GetMapping("/listRoleIdsByUser")
    fun listRoleIdsByUser(@RequestParam userId: String): Set<String> =
        authRoleUserService.getRoleIdsByUserId(userId)

    /** 批量给角色授予用户。返回新增的关系数。 */
    @PostMapping("/bindUsers")
    fun bindUsers(@RequestParam roleId: String, @RequestBody userIds: Collection<String>): Int =
        authRoleUserService.batchBind(roleId, userIds)

    /** 解绑角色↔用户关系。 */
    @DeleteMapping("/unbindUser")
    fun unbindUser(@RequestParam roleId: String, @RequestParam userId: String): Boolean =
        authRoleUserService.unbind(roleId, userId)

    /** 列出指定角色的资源 ID */
    @GetMapping("/listResourceIds")
    fun listResourceIds(@RequestParam roleId: String): Set<String> =
        authRoleResourceService.getResourceIdsByRoleId(roleId)

    /** 列出引用了指定资源的角色 ID */
    @GetMapping("/listRoleIdsByResource")
    fun listRoleIdsByResource(@RequestParam resourceId: String): Set<String> =
        authRoleResourceService.getRoleIdsByResourceId(resourceId)

    /** 批量给角色授权资源。返回新增的关系数。 */
    @PostMapping("/bindResources")
    fun bindResources(@RequestParam roleId: String, @RequestBody resourceIds: Collection<String>): Int =
        authRoleResourceService.batchBind(roleId, resourceIds)

    /** 解绑角色↔资源关系。 */
    @DeleteMapping("/unbindResource")
    fun unbindResource(@RequestParam roleId: String, @RequestParam resourceId: String): Boolean =
        authRoleResourceService.unbind(roleId, resourceId)

}
