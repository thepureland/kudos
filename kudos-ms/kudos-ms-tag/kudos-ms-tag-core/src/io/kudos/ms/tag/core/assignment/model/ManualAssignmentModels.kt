package io.kudos.ms.tag.core.assignment.model

import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.runtime.port.AssignmentDelta
import java.time.Instant

data class AssignManualTagCommand(
    val eventId: String,
    val requestId: String?,
    val subjectKey: TagSubjectKey,
    val tagId: String,
    val operatorId: String,
    val operatorName: String?,
    val reason: String?,
    val effectiveUntil: Instant?,
    val occurredAt: Instant,
)

data class RemoveManualTagCommand(
    val eventId: String,
    val requestId: String?,
    val subjectKey: TagSubjectKey,
    val tagId: String,
    val manualSourceRef: String,
    val operatorId: String,
    val operatorName: String?,
    val reason: String?,
    val occurredAt: Instant,
)

enum class ManualAssignmentStatus { APPLIED, DUPLICATE }

data class ManualAssignmentResult(
    val eventId: String,
    val status: ManualAssignmentStatus,
    val delta: AssignmentDelta,
)

class ManualAssignmentValidationException(
    val errorCode: TagErrorCode,
    message: String,
) : IllegalArgumentException(message)
