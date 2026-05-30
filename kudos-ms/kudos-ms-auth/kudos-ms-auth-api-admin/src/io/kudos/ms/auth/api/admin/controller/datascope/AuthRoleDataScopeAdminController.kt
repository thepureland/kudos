package io.kudos.ms.auth.api.admin.controller.datascope

import io.kudos.ms.auth.common.datascope.vo.request.AuthRoleOrgBindRequest
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeVo
import io.kudos.ms.auth.core.role.datascope.service.iservice.IAuthRoleDataScopeService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Data-scope (数据权限) administration controller.
 *
 * Base URL: `/api/admin/auth/roleDataScope`
 *
 * The role's `data_scope` value itself is edited through the normal role form
 * (AuthRoleFormCreate/Update → AuthRoleAdminController). This controller only manages the
 * CUSTOM-scope org grants and exposes the per-user resolution that business services consume.
 *
 *   GET  /getRoleOrgIds?roleId=...   → Set<String>  custom org ids for the role
 *   POST /bindRoleOrgs   body={ roleId, orgIds }    → Int  grants persisted (replace semantics)
 *   GET  /resolve?userId=...         → DataScopeVo   the user's effective row-visibility policy
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/roleDataScope")
class AuthRoleDataScopeAdminController(
    private val service: IAuthRoleDataScopeService,
) {

    /** Custom data-scope org ids granted to a role (only meaningful when its data_scope = CUSTOM). */
    @GetMapping("/getRoleOrgIds")
    fun getRoleOrgIds(@RequestParam roleId: String): Set<String> =
        service.getOrgIdsByRoleId(roleId)

    /** Set a role's custom org grants (replace semantics). Returns the count persisted. */
    @PostMapping("/bindRoleOrgs")
    fun bindRoleOrgs(@RequestBody request: AuthRoleOrgBindRequest): Int =
        service.bindOrgs(request.roleId, request.orgIds)

    /** Resolve a user's effective data scope across all their roles (most permissive wins). */
    @GetMapping("/resolve")
    fun resolve(@RequestParam userId: String): DataScopeVo =
        service.resolveUserDataScope(userId)
}
