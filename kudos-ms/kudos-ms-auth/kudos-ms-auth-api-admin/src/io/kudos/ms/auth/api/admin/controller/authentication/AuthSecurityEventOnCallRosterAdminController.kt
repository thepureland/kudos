package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.base.enums.impl.YesNotEnum
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventOnCallRosterAdminResponse
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventOnCallRosterAdminSaveRequest
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventOnCallShiftAdminResponse
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRoster
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRosterSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallShiftCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound management of security-event on-call rotations, with optimistic concurrency and audit. */
@RestController
@RequestMapping("/api/admin/auth/securityEventOnCallRosters")
open class AuthSecurityEventOnCallRosterAdminController(
    private val service: IAuthSecurityEventOnCallRosterService,
    private val userAccountService: IUserAccountService,
) {

    @GetMapping("/list")
    @RequiresPermission(VIEW_PERMISSION)
    open fun list(): List<AuthSecurityEventOnCallRosterAdminResponse> = translateErrors {
        val operator = currentOperator()
        service.listByTenant(operator.tenantId).map { it.toResponse() }
    }

    @PostMapping("/save")
    @RequiresPermission(UPDATE_PERMISSION)
    @WebAudit(
        opType = OperationTypeEnum.UPDATE,
        moduleCode = MODULE_CODE,
        desc = "保存租户安全事件值班表",
        ignoreForm = YesNotEnum.NOT,
    )
    open fun save(
        @RequestBody request: AuthSecurityEventOnCallRosterAdminSaveRequest,
    ): AuthSecurityEventOnCallRosterAdminResponse = translateErrors {
        val operator = currentOperator()
        if (request.shifts.size > MAX_SHIFTS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "shifts must not exceed $MAX_SHIFTS entries")
        }
        val shifts = request.shifts.map { shift ->
            val responderUserId = shift.responderUserId.trim().also {
                if (it.isBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "responderUserId must not be blank")
                }
            }
            AuthSecurityEventOnCallShiftCommand(responderUserId, shift.tier, shift.startAt, shift.endAt)
        }
        // A responder is only addressable if the current administrator's own tenant owns the account; an id
        // from another tenant is reported as absent rather than confirmed to exist.
        shifts.map { it.responderUserId }.distinct().forEach { requireTenantUser(it, operator.tenantId) }
        service.save(
            AuthSecurityEventOnCallRosterSaveCommand(
                tenantId = operator.tenantId,
                rosterCode = request.rosterCode,
                displayName = request.displayName,
                enabled = request.enabled,
                shifts = shifts,
                expectedVersion = request.expectedVersion,
                actorUserId = operator.id,
                reason = request.reason,
            )
        ).toResponse()
    }

    private fun AuthSecurityEventOnCallRoster.toResponse() = AuthSecurityEventOnCallRosterAdminResponse(
        rosterCode = rosterCode,
        displayName = displayName,
        enabled = enabled,
        shifts = shifts.map {
            AuthSecurityEventOnCallShiftAdminResponse(
                responderUserId = it.responderUserId,
                tier = it.tier,
                startAt = it.startAt,
                endAt = it.endAt,
            )
        },
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
            "AUTH_SECURITY_EVENT_ONCALL_VERSION_CONFLICT" -> HttpStatus.CONFLICT
            else -> HttpStatus.BAD_REQUEST
        }
        throw ResponseStatusException(status, e.errorCode, e)
    }

    private companion object {
        const val MODULE_CODE = "auth-security-event-oncall"
        const val VIEW_PERMISSION = "auth:security-event-oncall:view"
        const val UPDATE_PERMISSION = "auth:security-event-oncall:update"
        const val MAX_SHIFTS = 100
    }
}
