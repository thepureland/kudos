package io.kudos.ms.auth.core.authentication.mfa.store

import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollment
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

/**
 * Single-node pending enrollment store.
 *
 * The lazy expiry in [get] stands in for the TTL that Redis applies server-side in the distributed store, so
 * both implementations answer the same way once an enrollment runs out. It reads the injected [clock] rather
 * than the wall clock: a store that decides expiry from `Instant.now()` cannot agree with a caller holding a
 * fixed clock, which made the enrollment service's own tests pass or fail depending on the time of day they ran.
 */
open class InMemoryTotpEnrollmentStore(
    private val clock: Clock = Clock.systemUTC(),
) : ITotpEnrollmentStore {
    private val enrollments = ConcurrentHashMap<String, TotpEnrollment>()

    override fun create(enrollment: TotpEnrollment): Boolean =
        enrollments.putIfAbsent(enrollment.id, enrollment) == null

    override fun get(id: String): TotpEnrollment? {
        val enrollment = enrollments[id] ?: return null
        if (!clock.instant().isBefore(enrollment.expiresAt)) {
            enrollments.remove(id, enrollment)
            return null
        }
        return enrollment
    }

    override fun save(enrollment: TotpEnrollment, expectedVersion: Long): TotpEnrollment? {
        var saved: TotpEnrollment? = null
        enrollments.computeIfPresent(enrollment.id) { _, current ->
            if (current.version != expectedVersion) current
            else enrollment.copy(version = expectedVersion + 1).also { saved = it }
        }
        return saved
    }

    override fun consume(id: String, expectedVersion: Long): TotpEnrollment? {
        var consumed: TotpEnrollment? = null
        enrollments.computeIfPresent(id) { _, current ->
            if (current.version != expectedVersion) current
            else {
                consumed = current
                null
            }
        }
        return consumed
    }
}
