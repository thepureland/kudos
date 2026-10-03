package io.kudos.ms.auth.api.admin.controller.organization

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ms.auth.common.organization.vo.ManagementRoleRequest
import io.kudos.ms.auth.common.organization.vo.MemberDefaultRolesRequest
import io.kudos.ms.auth.common.organization.vo.MemberRoleOverrideRequest
import io.kudos.ms.auth.common.organization.vo.OrganizationAdminDesignationRequest
import io.kudos.ms.auth.common.organization.vo.OrganizationMemberRequest
import io.kudos.ms.auth.common.organization.vo.OrganizationRequest
import io.kudos.ms.auth.common.organization.vo.TenantAssociationRequest
import io.kudos.ms.auth.common.organization.vo.TenantDissociationRequest
import io.kudos.ms.auth.common.organization.vo.TenantOpeningPreviewRequest
import io.kudos.ms.auth.common.organization.vo.TenantOpeningRequest
import io.kudos.ms.auth.core.organization.ManagementRoleKind
import io.kudos.ms.auth.core.organization.RoleOverrideAction
import io.kudos.ms.auth.core.organization.service.DefaultRolesPreview
import io.kudos.ms.auth.core.organization.service.ManagementRoleRow
import io.kudos.ms.auth.core.organization.service.MemberAuthorizationView
import io.kudos.ms.auth.core.organization.service.OrganizationAdminRow
import io.kudos.ms.auth.core.organization.service.OrganizationManagementException
import io.kudos.ms.auth.core.organization.service.OrganizationManagerService
import io.kudos.ms.auth.core.organization.service.OrganizationMemberRoleService
import io.kudos.ms.auth.core.organization.service.OrganizationOverview
import io.kudos.ms.auth.core.organization.service.OrganizationOverviewService
import io.kudos.ms.auth.core.organization.service.OrganizationRevisionConflictException
import io.kudos.ms.auth.core.organization.service.OrganizationRoleRow
import io.kudos.ms.auth.core.organization.service.OrganizationTenantService
import io.kudos.ms.auth.core.organization.service.TenantOpeningPreview
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Organization-mode administration: tenant ownership and opening, members' default roles and
 * per-tenant overrides, organization administrators and management roles.
 *
 * Every endpoint is reachable only through the organization management permissions; who may do what
 * inside is decided by the organization administration policy behind each service. Writes return the
 * organization's new revision.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/organization")
open class OrganizationAdminController(
    private val overview: OrganizationOverviewService,
    private val tenants: OrganizationTenantService,
    private val members: OrganizationMemberRoleService,
    private val managers: OrganizationManagerService,
) {

    @GetMapping("/overview")
    open fun overview(@RequestParam organizationId: String): OrganizationOverview = overview.overview(organizationId)

    @GetMapping("/roleCatalog")
    open fun roleCatalog(@RequestParam organizationId: String): List<OrganizationRoleRow> = members.roleCatalog(organizationId)

    // region tenants

    @PostMapping("/tenant/associate")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "tenant 关联组织")
    open fun associate(@RequestBody request: TenantAssociationRequest): Long =
        tenants.associate(request.tenantId, request.organizationId, request.reason)

    @PostMapping("/tenant/dissociate")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "tenant 解除组织关联")
    open fun dissociate(@RequestBody request: TenantDissociationRequest): Long = tenants.dissociate(request.tenantId, request.reason)

    @PostMapping("/tenant/preview")
    open fun previewOpening(@RequestBody request: TenantOpeningPreviewRequest): TenantOpeningPreview =
        tenants.preview(request.tenantId, request.open)

    @PostMapping("/tenant/setOpen")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "开放或改回未开放 tenant")
    open fun setOpen(@RequestBody request: TenantOpeningRequest): Long =
        tenants.setOpen(request.tenantId, request.open, request.expectedRevision, request.reason)

    // endregion

    // region members

    @PostMapping("/member/read")
    open fun readMember(@RequestBody request: OrganizationMemberRequest): MemberAuthorizationView =
        members.read(request.organizationId, request.userId)

    @PostMapping("/member/previewDefaults")
    open fun previewDefaults(@RequestBody request: MemberDefaultRolesRequest): DefaultRolesPreview =
        members.previewDefaults(request.organizationId, request.userId, request.roleIds)

    @PostMapping("/member/saveDefaults")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "修改成员默认角色")
    open fun saveDefaults(@RequestBody request: MemberDefaultRolesRequest): Long =
        members.saveDefaults(request.organizationId, request.userId, request.roleIds, request.expectedRevision, request.reason)

    @PostMapping("/member/saveOverride")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "设置成员 tenant 角色覆盖")
    open fun saveOverride(@RequestBody request: MemberRoleOverrideRequest): Long = members.saveOverride(
        request.organizationId, request.tenantId, request.userId, request.roleId,
        RoleOverrideAction.valueOf(requireNotNull(request.action) { "action is required" }),
        request.expectedRevision, request.reason,
    )

    @PostMapping("/member/removeOverride")
    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "移除成员 tenant 角色覆盖")
    open fun removeOverride(@RequestBody request: MemberRoleOverrideRequest): Long = members.removeOverride(
        request.organizationId, request.tenantId, request.userId, request.roleId, request.expectedRevision, request.reason,
    )

    // endregion

    // region administrators and management roles

    @PostMapping("/admin/list")
    open fun listAdmins(@RequestBody request: OrganizationRequest): List<OrganizationAdminRow> = managers.listAdmins(request.organizationId)

    @PostMapping("/admin/assign")
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "指定组织管理员")
    open fun assignAdmin(@RequestBody request: OrganizationAdminDesignationRequest): Long =
        managers.assignAdmin(request.organizationId, request.userId, request.reason)

    @PostMapping("/admin/revoke")
    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "撤销组织管理员")
    open fun revokeAdmin(@RequestBody request: OrganizationAdminDesignationRequest): Long =
        managers.revokeAdmin(request.organizationId, request.userId, request.reason)

    @PostMapping("/managementRole/list")
    open fun listManagementRoles(@RequestBody request: OrganizationRequest): List<ManagementRoleRow> =
        managers.listManagementRoles(request.organizationId)

    @PostMapping("/managementRole/grant")
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "授予管理角色")
    open fun grantManagementRole(@RequestBody request: ManagementRoleRequest): Long = managers.grantManagementRole(
        request.organizationId, request.userId, ManagementRoleKind.valueOf(request.roleKind), request.tenantIds, request.reason,
    )

    @PostMapping("/managementRole/revoke")
    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "撤销管理角色")
    open fun revokeManagementRole(@RequestBody request: ManagementRoleRequest): Long = managers.revokeManagementRole(
        request.organizationId, request.userId, ManagementRoleKind.valueOf(request.roleKind), request.tenantIds, request.reason,
    )

    // endregion

    @ExceptionHandler(OrganizationRevisionConflictException::class)
    open fun conflict(e: OrganizationRevisionConflictException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("code" to "AUTHZ_REVISION_CONFLICT", "currentRevision" to e.currentRevision))

    @ExceptionHandler(OrganizationManagementException::class)
    open fun forbidden(e: OrganizationManagementException): ResponseEntity<Map<String, Any?>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("code" to e.message, "message" to e.message))

    private companion object {
        const val MODULE_CODE = "auth-organization"
    }
}
