package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventNotificationRouteAdminResponse
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventNotificationRouteAdminSaveRequest
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteConfig
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice.IAuthSecurityEventNotificationRouteConfigService
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound management of security-event notification routing, with optimistic concurrency and audit. */
@RestController
@RequestMapping("/api/admin/auth/securityEventNotificationRoutes")
open class AuthSecurityEventNotificationRouteAdminController(
    private val service: IAuthSecurityEventNotificationRouteConfigService,
    private val userAccountService: IUserAccountService,
) {

    @GetMapping("/list")
    @RequiresPermission(VIEW_PERMISSION)
    open fun list(): List<AuthSecurityEventNotificationRouteAdminResponse> = translateErrors {
        val operator = currentOperator()
        service.listByTenant(operator.tenantId).map { it.toResponse() }
    }

    @PostMapping("/save")
    @RequiresPermission(UPDATE_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "保存租户安全事件通知路由",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun save(
        @RequestBody request: AuthSecurityEventNotificationRouteAdminSaveRequest,
    ): AuthSecurityEventNotificationRouteAdminResponse = translateErrors {
        val operator = currentOperator()
        val responderUserIds = request.responderUserIds.map { responder ->
            responder.trim().also {
                if (it.isBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "responderUserIds must not contain blanks")
                }
            }
        }.toSet()
        // A responder is only addressable if the current administrator's own tenant owns the account; an id
        // from another tenant is reported as absent rather than confirmed to exist.
        responderUserIds.forEach { requireTenantUser(it, operator.tenantId) }
        service.save(
            AuthSecurityEventNotificationRouteSaveCommand(
                tenantId = operator.tenantId,
                notificationType = request.notificationType,
                appliesTo = request.appliesTo,
                routeCode = request.routeCode,
                destination = request.destination,
                channels = request.channels,
                responderUserIds = responderUserIds,
                responderRosterCode = request.responderRosterCode,
                includeAssignee = request.includeAssignee,
                enabled = request.enabled,
                fallbackBehavior = request.fallbackBehavior,
                expectedVersion = request.expectedVersion,
                actorUserId = operator.id,
                reason = request.reason,
            )
        ).toResponse()
    }

    private fun AuthSecurityEventNotificationRouteConfig.toResponse() =
        AuthSecurityEventNotificationRouteAdminResponse(
            notificationType = notificationType.name,
            appliesTo = appliesTo.name,
            routeCode = routeCode,
            destination = destination.name,
            channels = channels.map { it.name }.toSortedSet(),
            responderUserIds = responderUserIds.toSortedSet(),
            responderRosterCode = responderRosterCode,
            includeAssignee = includeAssignee,
            enabled = enabled,
            fallbackBehavior = fallbackBehavior.name,
            configVersion = configVersion,
            createUserId = createUserId,
            createReason = createReason,
            createTime = createTime,
            updateUserId = updateUserId,
            updateReason = updateReason,
            updateTime = updateTime,
        )

    private fun requireTenantUser(userId: String, tenantId: String) {
        if (userAccountService.getUserRecord(userId)?.tenantId != tenantId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Target user was not found")
        }
    }

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: AuthSecurityEventException) {
        val status = when (e.errorCode) {
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_CONFLICT" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth-security-event-notification-route"
        const val VIEW_PERMISSION = "auth:security-event-notification-route:view"
        const val UPDATE_PERMISSION = "auth:security-event-notification-route:update"
    }
}
