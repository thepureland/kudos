package io.kudos.ms.auth.api.admin.controller.datascope

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ms.auth.common.datascope.vo.request.AuthRoleOrgBindRequest
import io.kudos.ms.auth.common.datascope.vo.request.AuthRoleScopeBindRequest
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeExplanationVo
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

    /** Update just a role's data-scope policy code (focused alternative to the full role form). */
    @PostMapping("/updateScope")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "更新角色数据范围类型")
    fun updateScope(
        @RequestParam roleId: String,
        @RequestParam(required = false) dataScope: String?,
    ): Boolean = service.updateScope(roleId, dataScope)

    /** Custom data-scope org ids granted to a role (only meaningful when its data_scope = CUSTOM). */
    @GetMapping("/getRoleOrgIds")
    fun getRoleOrgIds(@RequestParam roleId: String): Set<String> =
        service.getOrgIdsByRoleId(roleId)

    /** Set a role's custom org grants (replace semantics). Returns the count persisted. */
    @PostMapping("/bindRoleOrgs")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "绑定角色自定义机构范围")
    fun bindRoleOrgs(@RequestBody request: AuthRoleOrgBindRequest): Int =
        service.bindOrgs(request.roleId, request.orgIds)

    /**
     * The dimensions this deployment has providers for. The console reads it instead of hardcoding
     * `org`, so an application that registers `region` gets a screen for it for free.
     */
    @GetMapping("/listDimensions")
    fun listDimensions(): List<String> = service.getRegisteredDimensions()

    /** Every dimension a role has grants on, mapped to its values. */
    @GetMapping("/getRoleScopes")
    fun getRoleScopes(@RequestParam roleId: String): Map<String, Set<String>> =
        service.getAllScopes(roleId)

    /**
     * Set a role's grants on ONE dimension (replace semantics for that dimension only — editing a
     * role's orgs must not wipe the regions configured on another screen).
     */
    @PostMapping("/bindRoleScope")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "绑定角色维度数据范围")
    fun bindRoleScope(@RequestBody request: AuthRoleScopeBindRequest): Int =
        service.bindScope(request.roleId, request.dimension, request.values)

    /**
     * Why a subject sees what they see: the resolved scope plus the per-role breakdown.
     *
     * Resolution takes the most permissive policy across every effective role, so the outcome cannot
     * be derived from one role's configuration — which makes the usual question ("I restricted this
     * role and he still sees everything") undiagnosable without this.
     */
    @GetMapping("/explain")
    fun explain(@RequestParam userId: String): DataScopeExplanationVo =
        service.explainUserDataScope(userId)

    /** Resolve a user's effective data scope across all their roles (most permissive wins). */
    @GetMapping("/resolve")
    fun resolve(@RequestParam userId: String): DataScopeVo =
        service.resolveUserDataScope(userId)

    companion object {
        /** 审计日志的归属模块编码 */
        private const val MODULE_CODE = "auth-role-data-scope"
    }
}
