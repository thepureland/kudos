package io.kudos.ms.user.api.admin.controller.org

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.user.common.account.support.eraseCredentials
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
 * Organization admin controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/user/org")
class UserOrgAdminController :
    BaseCrudController<String, IUserOrgService, UserOrgQuery, UserOrgRow, UserOrgDetail, UserOrgEdit, UserOrgFormCreate, UserOrgFormUpdate>() {

    /** Update the active flag. */
    @PutMapping("/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean =
        service.updateActive(id, active)

    /** Move an organization (reparent or reorder). */
    @PostMapping("/moveOrg")
    fun moveOrg(
        @RequestParam id: String,
        @RequestParam(required = false) newParentId: String?,
        @RequestParam(required = false) newSortNum: Int?,
    ): Boolean = service.moveOrg(id, newParentId, newSortNum)

    /** Get the organization tree (by tenant, optional root). */
    @GetMapping("/getOrgTree")
    fun getOrgTree(
        @RequestParam tenantId: String,
        @RequestParam(required = false) parentId: String?,
    ): List<UserOrgTreeRow> = service.getOrgTree(tenantId, parentId)

    /** Get all users by organization id (including admins). Credential fields are erased before returning. */
    @GetMapping("/getOrgUsers")
    fun getOrgUsers(@RequestParam orgId: String): List<UserAccountCacheEntry> =
        service.getOrgUsers(orgId).map { it.eraseCredentials() }

    /** Get admin users by organization id. Credential fields are erased before returning. */
    @GetMapping("/getOrgAdmins")
    fun getOrgAdmins(@RequestParam orgId: String): List<UserAccountCacheEntry> =
        service.getOrgAdmins(orgId).map { it.eraseCredentials() }

    /** Get direct child organizations by organization id. */
    @GetMapping("/getChildOrgs")
    fun getChildOrgs(@RequestParam orgId: String): List<UserOrgCacheEntry> =
        service.getChildOrgs(orgId)

    /** Get the parent organization by organization id. */
    @GetMapping("/getParentOrg")
    fun getParentOrg(@RequestParam orgId: String): UserOrgCacheEntry? =
        service.getParentOrg(orgId)

}
