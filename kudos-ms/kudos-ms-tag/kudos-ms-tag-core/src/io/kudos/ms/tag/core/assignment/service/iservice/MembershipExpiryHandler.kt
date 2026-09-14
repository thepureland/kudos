package io.kudos.ms.tag.core.assignment.service.iservice

import io.kudos.ms.tag.core.runtime.port.AssignmentDelta
import java.time.Instant

/** Narrow maintenance port kept separate from the public/manual assignment service contract. */
fun interface MembershipExpiryHandler {
    fun expireMembership(membershipId: String, expiredAt: Instant): AssignmentDelta?
}
