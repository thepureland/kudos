package io.kudos.ms.user.api.admin.controller.org

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.org.vo.request.UserOrgFormCreate
import io.kudos.ms.user.common.org.vo.request.UserOrgFormUpdate
import io.kudos.ms.user.common.org.vo.request.UserOrgQuery
import io.kudos.ms.user.common.org.vo.response.UserOrgDetail
import io.kudos.ms.user.common.org.vo.response.UserOrgEdit
import io.kudos.ms.user.common.org.vo.response.UserOrgRow
import io.kudos.ms.user.common.org.vo.response.UserOrgTreeRow
import io.kudos.ms.user.core.org.service.iservice.IUserOrgService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * 机构管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/user/org")
class UserOrgAdminController :
    BaseCrudController<String, IUserOrgService, UserOrgQuery, UserOrgRow, UserOrgDetail, UserOrgEdit, UserOrgFormCreate, UserOrgFormUpdate>() {

    /** 更新启用状态 */
    @PutMapping("/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean =
        service.updateActive(id, active)

    /** 移动机构（改父或调序） */
    @PostMapping("/moveOrg")
    fun moveOrg(
        @RequestParam id: String,
        @RequestParam(required = false) newParentId: String?,
        @RequestParam(required = false) newSortNum: Int?,
    ): Boolean = service.moveOrg(id, newParentId, newSortNum)

    /** 获取机构树（按租户，可指定根） */
    @GetMapping("/getOrgTree")
    fun getOrgTree(
        @RequestParam tenantId: String,
        @RequestParam(required = false) parentId: String?,
    ): List<UserOrgTreeRow> = service.getOrgTree(tenantId, parentId)

    /** 按机构 ID 取所有用户（含管理员） */
    @GetMapping("/getOrgUsers")
    fun getOrgUsers(@RequestParam orgId: String): List<UserAccountCacheEntry> =
        service.getOrgUsers(orgId)

    /** 按机构 ID 取管理员用户 */
    @GetMapping("/getOrgAdmins")
    fun getOrgAdmins(@RequestParam orgId: String): List<UserAccountCacheEntry> =
        service.getOrgAdmins(orgId)

    /** 按机构 ID 取直接子机构 */
    @GetMapping("/getChildOrgs")
    fun getChildOrgs(@RequestParam orgId: String): List<UserOrgCacheEntry> =
        service.getChildOrgs(orgId)

    /** 按机构 ID 取父机构 */
    @GetMapping("/getParentOrg")
    fun getParentOrg(@RequestParam orgId: String): UserOrgCacheEntry? =
        service.getParentOrg(orgId)

}
