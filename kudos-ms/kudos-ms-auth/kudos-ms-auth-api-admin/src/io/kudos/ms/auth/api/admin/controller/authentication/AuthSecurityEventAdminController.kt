package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventAcknowledgeRequest
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventAssignRequest
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventCloseRequest
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAcknowledgeCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAssignCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventCloseCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventSummary
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventService
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound security event query and explicit, audit-tracked workflow transitions. */
@RestController
@RequestMapping("/api/admin/auth/securityEvents")
open class AuthSecurityEventAdminController(
    private val securityEventService: IAuthSecurityEventService,
    private val userAccountService: IUserAccountService,
) {

    @GetMapping
    @RequiresPermission(VIEW_PERMISSION)
    open fun list(
        @RequestParam(required = false) userId: String? = null,
        @RequestParam(required = false) riskLevel: String? = null,
        @RequestParam(required = false) status: String? = null,
        @RequestParam(required = false) assigneeUserId: String? = null,
        @RequestParam(defaultValue = "false") overdueOnly: Boolean = false,
        @RequestParam(defaultValue = "100") limit: Int = 100,
    ): List<AuthSecurityEventSummary> {
        val operator = CurrentUserKit.currentPrincipalOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")
        if (limit !in 1..MAX_LIMIT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and $MAX_LIMIT")
        }
        val targetUserId = userId?.trim()?.also {
            if (it.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank")
        }
        val targetAssigneeUserId = assigneeUserId?.trim()?.also {
            if (it.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "assigneeUserId must not be blank")
        }
        targetUserId?.let { requireTenantUser(it, operator.tenantId) }
        targetAssigneeUserId?.let { requireTenantUser(it, operator.tenantId) }
        return try {
            securityEventService.listRecent(
                tenantId = operator.tenantId,
                userId = targetUserId,
                riskLevel = riskLevel?.trim(),
                status = status?.trim(),
                assigneeUserId = targetAssigneeUserId,
                overdueOnly = overdueOnly,
                limit = limit,
            )
        } catch (e: AuthSecurityEventException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.errorCode, e)
        }
    }

    @PostMapping("/{id}/assign")
    @RequiresPermission(ASSIGN_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "分派认证安全事件",
    )
    open fun assign(
        @PathVariable id: String,
        @RequestBody request: AuthSecurityEventAssignRequest,
    ): AuthSecurityEventSummary = translateWorkflowErrors {
        val operator = currentOperator()
        val assigneeUserId = request.assigneeUserId.trim().also {
            if (it.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "assigneeUserId must not be blank")
        }
        requireTenantUser(assigneeUserId, operator.tenantId)
        securityEventService.assign(
            AuthSecurityEventAssignCommand(
                tenantId = operator.tenantId,
                eventId = id,
                actorUserId = operator.id,
                assigneeUserId = assigneeUserId,
                reason = request.reason,
                expectedVersion = request.expectedVersion,
            )
        )
    }

    @PostMapping("/{id}/acknowledge")
    @RequiresPermission(ACKNOWLEDGE_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "确认认证安全事件",
    )
    open fun acknowledge(
        @PathVariable id: String,
        @RequestBody request: AuthSecurityEventAcknowledgeRequest,
    ): AuthSecurityEventSummary = translateWorkflowErrors {
        val operator = currentOperator()
        securityEventService.acknowledge(
            AuthSecurityEventAcknowledgeCommand(
                tenantId = operator.tenantId,
                eventId = id,
                actorUserId = operator.id,
                reason = request.reason,
                expectedVersion = request.expectedVersion,
            )
        )
    }

    @PostMapping("/{id}/close")
    @RequiresPermission(CLOSE_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "关闭认证安全事件",
    )
    open fun close(
        @PathVariable id: String,
        @RequestBody request: AuthSecurityEventCloseRequest,
    ): AuthSecurityEventSummary = translateWorkflowErrors {
        val operator = currentOperator()
        securityEventService.close(
            AuthSecurityEventCloseCommand(
                tenantId = operator.tenantId,
                eventId = id,
                actorUserId = operator.id,
                resolution = request.resolution,
                reason = request.reason,
                expectedVersion = request.expectedVersion,
            )
        )
    }

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun requireTenantUser(userId: String, tenantId: String) {
        if (userAccountService.getUserRecord(userId)?.tenantId != tenantId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Target user was not found")
        }
    }

    private fun <T> translateWorkflowErrors(block: () -> T): T = try {
        block()
    } catch (e: AuthSecurityEventException) {
        val status = when (e.errorCode) {
            "AUTH_SECURITY_EVENT_NOT_FOUND" -> HttpStatus.NOT_FOUND
            "AUTH_SECURITY_EVENT_BUCKET_NOT_SETTLED",
            "AUTH_SECURITY_EVENT_VERSION_CONFLICT",
            "AUTH_SECURITY_EVENT_STATE_CONFLICT",
            "AUTH_SECURITY_EVENT_TRANSITION_CONFLICT",
            -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val VIEW_PERMISSION = "auth:security-event:view"
        const val ACKNOWLEDGE_PERMISSION = "auth:security-event:acknowledge"
        const val CLOSE_PERMISSION = "auth:security-event:close"
        const val ASSIGN_PERMISSION = "auth:security-event:assign"
        const val MODULE_CODE = "auth-security-event"
        const val MAX_LIMIT = 500
    }
}
