package io.kudos.ms.auth.core.authentication.securityevent.oncall

import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.spi.IAuthSecurityEventResponderResolver
import java.time.LocalDateTime

/**
 * The built-in resolver: reads the tenant's stored rotation and answers with whoever it puts on call.
 *
 * **Tiers and escalation levels.** A level-1 escalation reaches tier 1, a level-2 escalation reaches tiers 1
 * and 2, and so on — escalating adds people rather than handing over to them, because the first responder
 * being unreachable is exactly the case where the incident still needs them to see it. Tiers above the level
 * are left alone, so a tenant can park a manager at tier 3 without paging them for every routine deadline.
 */
open class OnCallScheduleResponderResolver(
    private val rosterService: IAuthSecurityEventOnCallRosterService,
) : IAuthSecurityEventResponderResolver {

    override fun resolve(
        tenantId: String,
        rosterCode: String,
        escalationLevel: Int,
        at: LocalDateTime,
    ): Set<String> {
        val roster = rosterService.findEnabled(tenantId, rosterCode) ?: return emptySet()
        return roster.shifts
            .filter { it.tier <= escalationLevel && it.covers(at) }
            .map { it.responderUserId }
            .toSet()
    }
}
