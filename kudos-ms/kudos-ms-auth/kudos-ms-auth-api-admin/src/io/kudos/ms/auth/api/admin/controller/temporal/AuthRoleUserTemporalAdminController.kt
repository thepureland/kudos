package io.kudos.ms.auth.api.admin.controller.temporal

import io.kudos.ms.auth.common.temporal.vo.request.AuthRoleUserTemporalBindRequest
import io.kudos.ms.auth.core.role.temporal.service.iservice.IAuthRoleUserTemporalService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Temporal (时效性) role-grant administration controller.
 *
 * Base URL: `/api/admin/auth/roleUserTemporal`
 *
 *   POST /bind          body={ roleId, userId, startTime?, endTime? }  → new grant id (replace)
 *   POST /purgeExpired                                                  → count of grants purged
 *
 * The role↔user effective-now filtering lives in the DAO/caches; `purgeExpired` is the reaper that
 * deletes lapsed grants and evicts caches — intended to be triggered periodically by a scheduler.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/roleUserTemporal")
class AuthRoleUserTemporalAdminController(
    private val service: IAuthRoleUserTemporalService,
) {

    /** Grant a role to a user with an optional validity window (replace semantics). */
    @PostMapping("/bind")
    fun bind(@RequestBody request: AuthRoleUserTemporalBindRequest): String =
        service.bindTemporal(request.roleId, request.userId, request.startTime, request.endTime)

    /** Delete all expired grants and evict the affected users' caches. Returns the purged count. */
    @PostMapping("/purgeExpired")
    fun purgeExpired(): Int = service.purgeExpired()
}
