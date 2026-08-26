package io.kudos.ms.auth.core.authentication.securityevent.oncall

import java.io.Serializable
import java.time.LocalDateTime

/**
 * One rotation entry: a responder is on call between two UTC instants at a given tier.
 *
 * Deliberately an explicit window rather than a recurrence rule. A rule engine would have to own timezones,
 * daylight saving, holiday calendars and overrides before it could be trusted with who gets woken up at 3am;
 * an explicit window is something an operator can read, an external planner can generate, and a test can pin.
 */
data class AuthSecurityEventOnCallShift(
    val id: String,
    val responderUserId: String,
    val tier: Int,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
) : Serializable {
    /** Half-open on the end so back-to-back shifts hand over without both matching the same instant. */
    fun covers(at: LocalDateTime): Boolean = !at.isBefore(startAt) && at.isBefore(endAt)

    companion object {
        private const val serialVersionUID = 1L
    }
}

/** A tenant's named rotation, cached across nodes together with its shifts. */
data class AuthSecurityEventOnCallRoster(
    val tenantId: String,
    val rosterCode: String,
    val displayName: String,
    val enabled: Boolean,
    val shifts: List<AuthSecurityEventOnCallShift>,
    val configVersion: Long,
    val createUserId: String,
    val createReason: String,
    val createTime: LocalDateTime,
    val updateUserId: String,
    val updateReason: String,
    val updateTime: LocalDateTime,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Replaces one roster and its whole shift set under a single version check.
 *
 * Whole-set replacement rather than per-shift CRUD: a rotation is only meaningful as a set, and one version
 * per save means two administrators editing the same week cannot interleave into a roster neither of them wrote.
 */
data class AuthSecurityEventOnCallRosterSaveCommand(
    val tenantId: String,
    val rosterCode: String,
    val displayName: String,
    val enabled: Boolean,
    val shifts: List<AuthSecurityEventOnCallShiftCommand>,
    val expectedVersion: Long,
    val actorUserId: String,
    val reason: String,
)

data class AuthSecurityEventOnCallShiftCommand(
    val responderUserId: String,
    val tier: Int,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
)

/** One appended roster change record, holding the replaced rotation rather than only the current one. */
data class AuthSecurityEventOnCallRosterAuditRecord(
    val id: String,
    val tenantId: String,
    val rosterId: String,
    val actorUserId: String,
    val reason: String,
    val configVersion: Long,
    val beforeSnapshot: String?,
    val afterSnapshot: String,
    val changedAt: LocalDateTime,
)
