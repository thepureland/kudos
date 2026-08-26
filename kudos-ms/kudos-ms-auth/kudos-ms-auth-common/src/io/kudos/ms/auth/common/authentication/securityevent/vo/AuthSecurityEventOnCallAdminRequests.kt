package io.kudos.ms.auth.common.authentication.securityevent.vo

import java.time.LocalDateTime

/**
 * Replaces one rotation and its whole shift set.
 *
 * Deliberately without a tenant or an operator field: both are taken from the trusted administrator context.
 */
data class AuthSecurityEventOnCallRosterAdminSaveRequest(
    val rosterCode: String,
    val displayName: String,
    val enabled: Boolean = true,
    val shifts: List<AuthSecurityEventOnCallShiftAdminRequest> = emptyList(),
    /** `0` creates the rotation; otherwise the `configVersion` the page was rendered from. */
    val expectedVersion: Long,
    val reason: String,
)

/** One rotation entry; the window is a UTC instant pair, not a local-time recurrence. */
data class AuthSecurityEventOnCallShiftAdminRequest(
    val responderUserId: String,
    val tier: Int,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
)

data class AuthSecurityEventOnCallRosterAdminResponse(
    val rosterCode: String,
    val displayName: String,
    val enabled: Boolean,
    val shifts: List<AuthSecurityEventOnCallShiftAdminResponse>,
    val configVersion: Long,
    val createUserId: String,
    val createReason: String,
    val createTime: LocalDateTime,
    val updateUserId: String,
    val updateReason: String,
    val updateTime: LocalDateTime,
)

data class AuthSecurityEventOnCallShiftAdminResponse(
    val responderUserId: String,
    val tier: Int,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
)
