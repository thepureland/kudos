package io.kudos.ms.auth.core.authentication.securityevent.event

import java.time.LocalDateTime

/** Notification extension point emitted only after an active event assignment transaction commits. */
data class AuthSecurityEventAssigned(
    val tenantId: String,
    val eventId: String,
    val actorUserId: String,
    val assigneeUserId: String,
    val previousAssigneeUserId: String?,
    val dueAt: LocalDateTime?,
    val occurredAt: LocalDateTime,
)
