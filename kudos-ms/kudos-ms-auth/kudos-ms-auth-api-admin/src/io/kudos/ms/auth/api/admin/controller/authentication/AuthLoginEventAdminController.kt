package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEvent
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventException
import io.kudos.ms.auth.core.authentication.loginevent.service.iservice.IAuthLoginEventService
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Tenant-bound, read-only authentication outcome audit. */
@RestController
@RequestMapping("/api/admin/auth/loginEvents")
open class AuthLoginEventAdminController(
    private val service: IAuthLoginEventService,
    private val userAccountService: IUserAccountService,
) {

    /**
     * [identifier] is the plain attempted name an operator would type; it is hashed before it reaches the
     * query, so the audit can be searched without the column ever holding a readable username.
     */
    @GetMapping
    @RequiresPermission(VIEW_PERMISSION)
    open fun list(
        @RequestParam(required = false) userId: String? = null,
        @RequestParam(required = false) identifier: String? = null,
        @RequestParam(required = false) successOnly: Boolean? = null,
        @RequestParam(defaultValue = "100") limit: Int = 100,
    ): List<AuthLoginEvent> = translateErrors {
        val operator = CurrentUserKit.currentPrincipalOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "An authenticated administrator is required")
        if (limit !in 1..MAX_LIMIT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and $MAX_LIMIT")
        }
        val targetUserId = userId?.trim()?.also {
            if (it.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank")
            if (userAccountService.getUserRecord(it)?.tenantId != operator.tenantId) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Target user was not found")
            }
        }
        service.listRecent(
            tenantId = operator.tenantId,
            userId = targetUserId,
            identifier = identifier?.trim()?.takeIf { it.isNotEmpty() },
            successOnly = successOnly,
            limit = limit,
        )
    }

    private fun <T> translateErrors(block: () -> T): T = try {
        block()
    } catch (e: AuthLoginEventException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.errorCode, e)
    }

    private companion object {
        const val VIEW_PERMISSION = "auth:login-event:view"
        const val MAX_LIMIT = 500
    }
}
