package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventNotificationReplayRequest
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationReplayCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationSummary
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationAdminService
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound inspection and explicitly audited replay of dead security-event notifications. */
@RestController
@RequestMapping("/api/admin/auth/securityEventNotifications")
open class AuthSecurityEventNotificationAdminController(
    private val service: IAuthSecurityEventNotificationAdminService,
) {

    @GetMapping("/dead")
    @RequiresPermission(VIEW_PERMISSION)
    open fun listDead(
        @RequestParam(required = false) eventId: String? = null,
        @RequestParam(defaultValue = "100") limit: Int = 100,
    ): List<AuthSecurityEventNotificationSummary> = translateErrors {
        val operator = currentOperator()
        val normalizedEventId = eventId?.trim()?.also {
            if (it.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId must not be blank")
        }
        if (limit !in 1..MAX_LIMIT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and $MAX_LIMIT")
        }
        service.listDead(operator.tenantId, normalizedEventId, limit)
    }

    @PostMapping("/{id}/replay")
    @RequiresPermission(REPLAY_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "重放认证安全事件通知",
    )
    open fun replay(
        @PathVariable id: String,
        @RequestBody request: AuthSecurityEventNotificationReplayRequest,
    ): AuthSecurityEventNotificationSummary = translateErrors {
        val operator = currentOperator()
        service.replay(
            AuthSecurityEventNotificationReplayCommand(
                tenantId = operator.tenantId,
                notificationId = id,
                actorUserId = operator.id,
                reason = request.reason,
            )
        )
    }

    private fun currentOperator() = CurrentUserKit.currentPrincipalOrNull()
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: AuthSecurityEventException) {
        val status = when (e.errorCode) {
            "AUTH_SECURITY_EVENT_NOTIFICATION_NOT_FOUND" -> HttpStatus.NOT_FOUND
            "AUTH_SECURITY_EVENT_NOTIFICATION_STATE_CONFLICT" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val VIEW_PERMISSION = "auth:security-event-notification:view"
        const val REPLAY_PERMISSION = "auth:security-event-notification:replay"
        const val MODULE_CODE = "auth-security-event-notification"
        const val MAX_LIMIT = 500
    }
}
