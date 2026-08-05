package io.kudos.ms.auth.api.admin.controller.tenant

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ms.auth.common.tenant.vo.TenantBootstrapResult
import io.kudos.ms.auth.common.tenant.vo.TenantBootstrapStatusVo
import io.kudos.ms.auth.core.tenant.service.iservice.ITenantBootstrapService
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * Getting a new tenant to the point where it can administer itself.
 *
 * Seeding normally happens by itself when the tenant is created; this exposes it for the case where
 * that failed, and — separately — for appointing the tenant's first administrator, which is a
 * decision rather than a derivation and so is never automatic.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/tenantBootstrap")
class TenantBootstrapAdminController {

    @Resource
    private lateinit var tenantBootstrapService: ITenantBootstrapService

    /**
     * Where the tenant is in the bootstrap: seeded or not, and who administers it. Both in one call,
     * because a console needs them together to know which of the two steps it is showing.
     */
    @GetMapping("/status")
    fun status(@RequestParam tenantId: String): TenantBootstrapStatusVo =
        tenantBootstrapService.status(tenantId)

    /**
     * Creates the tenant's configured built-in roles. Idempotent — an existing role is reported and
     * left untouched, bindings included.
     */
    @PostMapping("/seedRoles")
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "初始化租户引导角色")
    fun seedRoles(@RequestParam tenantId: String): TenantBootstrapResult =
        tenantBootstrapService.seedRoles(tenantId)

    /**
     * Appoints the tenant's first administrator.
     *
     * Refuses if the role already has holders unless [force] is passed: "first administrator" is a
     * one-time act, and an endpoint that quietly appoints a second one is an escalation path.
     */
    @PostMapping("/bindFirstAdministrator")
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "指派租户首位管理员")
    fun bindFirstAdministrator(
        @RequestParam tenantId: String,
        @RequestParam userId: String,
        @RequestParam(required = false) roleCode: String?,
        @RequestParam(required = false, defaultValue = "false") force: Boolean,
    ): TenantBootstrapResult =
        tenantBootstrapService.bindFirstAdministrator(tenantId, userId, roleCode, force)

    companion object {
        private const val MODULE_CODE = "auth-tenant-bootstrap"
    }
}
