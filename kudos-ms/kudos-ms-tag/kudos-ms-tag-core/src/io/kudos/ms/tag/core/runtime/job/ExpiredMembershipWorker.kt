package io.kudos.ms.tag.core.runtime.job

import io.kudos.ms.tag.core.assignment.service.iservice.MembershipExpiryHandler
import io.kudos.ms.tag.core.runtime.membership.dao.TagMembershipDao
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.ZoneOffset

/** Passive until both host scheduling and the expiry-specific property are enabled. */
@Component
open class ExpiredMembershipWorker(
    private val membershipDao: TagMembershipDao,
    private val assignmentService: MembershipExpiryHandler,
    private val properties: TagRecalculationProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    open fun runOnce(): Int {
        val now = clock.instant()
        val candidates = membershipDao.listExpiredActive(now.atOffset(ZoneOffset.UTC).toLocalDateTime(), properties.expiryBatchSize)
        candidates.forEach { assignmentService.expireMembership(it.id, now) }
        return candidates.size
    }

    @Scheduled(fixedDelayString = "\${kudos.tag.recalculation.expiry-poll-delay:1m}")
    open fun poll() {
        if (properties.expirySchedulingEnabled) runOnce()
    }
}
