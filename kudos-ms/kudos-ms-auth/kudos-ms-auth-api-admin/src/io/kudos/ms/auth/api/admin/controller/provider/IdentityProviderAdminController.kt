package io.kudos.ms.auth.api.admin.controller.provider

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderAdminCreateRequest
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderAdminSetActiveRequest
import io.kudos.ms.auth.common.provider.vo.request.IdentityProviderAdminUpdateRequest
import io.kudos.ms.auth.common.provider.vo.response.IdentityProviderAdminResponse
import io.kudos.ms.auth.common.provider.vo.response.IdentityProviderTemplateAdminResponse
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderCreateCommand
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderManagementException
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderSetActiveCommand
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderUpdateCommand
import io.kudos.ms.auth.core.provider.management.model.ManagedIdentityProvider
import io.kudos.ms.auth.core.provider.management.model.ManagedProviderTemplate
import io.kudos.ms.auth.core.provider.management.service.iservice.IIdentityProviderManagementService
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/auth/identityProvider")
open class IdentityProviderAdminController(
    private val service: IIdentityProviderManagementService,
) {

    @GetMapping("/listTemplates")
    @RequiresPermission(VIEW_PERMISSION)
    open fun listTemplates(): List<IdentityProviderTemplateAdminResponse> =
        service.listTemplates().map { it.toResponse() }

    @GetMapping("/list")
    @RequiresPermission(VIEW_PERMISSION)
    open fun list(): List<IdentityProviderAdminResponse> {
        val operator = currentOperator()
        return service.list(operator.tenantId).map { it.toResponse() }
    }

    @GetMapping("/get")
    @RequiresPermission(VIEW_PERMISSION)
    open fun get(@RequestParam providerId: String): IdentityProviderAdminResponse = translateErrors {
        val operator = currentOperator()
        service.get(providerId, operator.tenantId).toResponse()
    }

    @PostMapping("/create")
    @RequiresPermission("auth:identity-provider:create")
    @WebAudit(
        opType = OperationTypeEnum.CREATE,
        moduleCode = MODULE_CODE,
        desc = "创建外部身份 Provider",
        ignoreForm = YesNotEnum.YES,
    )
    open fun create(@RequestBody request: IdentityProviderAdminCreateRequest): IdentityProviderAdminResponse =
        translateErrors {
            val operator = currentOperator()
            service.create(
                IdentityProviderCreateCommand(
                    tenantId = operator.tenantId,
                    templateId = request.templateId,
                    code = request.code,
                    displayName = request.displayName,
                    issuer = request.issuer,
                    clientId = request.clientId,
                    clientSecretRef = request.clientSecretRef,
                    scopes = request.scopes,
                    jitPolicy = request.jitPolicy,
                    linkPolicy = request.linkPolicy,
                    active = request.active,
                    actorUserId = operator.id,
                    operationReason = request.reason,
                )
            ).toResponse()
        }

    @PostMapping("/update")
    @RequiresPermission("auth:identity-provider:update")
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "更新外部身份 Provider",
        ignoreForm = YesNotEnum.YES,
    )
    open fun update(@RequestBody request: IdentityProviderAdminUpdateRequest): IdentityProviderAdminResponse =
        translateErrors {
            val operator = currentOperator()
            service.update(
                IdentityProviderUpdateCommand(
                    providerId = request.providerId,
                    tenantId = operator.tenantId,
                    displayName = request.displayName,
                    issuer = request.issuer,
                    clientId = request.clientId,
                    clientSecretRef = request.clientSecretRef,
                    clearClientSecretRef = request.clearClientSecretRef,
                    scopes = request.scopes,
                    jitPolicy = request.jitPolicy,
                    linkPolicy = request.linkPolicy,
                    actorUserId = operator.id,
                    operationReason = request.reason,
                )
            ).toResponse()
        }

    @PostMapping("/setActive")
    @RequiresPermission("auth:identity-provider:set-active")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "启用或停用外部身份 Provider")
    open fun setActive(
        @RequestBody request: IdentityProviderAdminSetActiveRequest,
    ): IdentityProviderAdminResponse = translateErrors {
        val operator = currentOperator()
        service.setActive(
            IdentityProviderSetActiveCommand(
                providerId = request.providerId,
                tenantId = operator.tenantId,
                active = request.active,
                actorUserId = operator.id,
                operationReason = request.reason,
            )
        ).toResponse()
    }

    private fun ManagedProviderTemplate.toResponse() = IdentityProviderTemplateAdminResponse(
        id, code, protocol, issuer, defaultScopes, logoUri,
    )

    private fun ManagedIdentityProvider.toResponse() = IdentityProviderAdminResponse(
        id = id,
        templateId = templateId,
        templateCode = templateCode,
        protocol = protocol,
        code = code,
        displayName = displayName,
        issuer = issuer,
        clientId = clientId,
        clientSecretConfigured = clientSecretConfigured,
        scopes = scopes,
        effectiveScopes = effectiveScopes,
        jitPolicy = jitPolicy,
        linkPolicy = linkPolicy,
        active = active,
    )

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: IdentityProviderManagementException) {
        val status = when (e.errorCode) {
            "EXTERNAL_PROVIDER_NOT_AVAILABLE" -> HttpStatus.NOT_FOUND
            "EXTERNAL_PROVIDER_TENANT_MISMATCH" -> HttpStatus.FORBIDDEN
            "EXTERNAL_PROVIDER_CODE_EXISTS",
            "EXTERNAL_PROVIDER_TEMPLATE_NOT_AVAILABLE",
            "EXTERNAL_PROVIDER_LINK_POLICY_NOT_SUPPORTED" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth"
        const val VIEW_PERMISSION = "auth:identity-provider:view"
    }
}
