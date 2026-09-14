package io.kudos.ms.tag.common.assignment.model

import io.kudos.ms.tag.common.attribute.model.InstantAsIsoStringSerializer
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AssignManualTagRequest(
    val eventId: String,
    val requestId: String?,
    val subjectKey: TagSubjectKey,
    val tagId: String,
    val operatorId: String,
    val operatorName: String?,
    val reason: String?,
    @Serializable(with = InstantAsIsoStringSerializer::class)
    val effectiveUntil: Instant?,
    @Serializable(with = InstantAsIsoStringSerializer::class)
    val occurredAt: Instant,
)

@Serializable
data class RemoveManualTagRequest(
    val eventId: String,
    val requestId: String?,
    val subjectKey: TagSubjectKey,
    val tagId: String,
    val manualSourceRef: String,
    val operatorId: String,
    val operatorName: String?,
    val reason: String?,
    @Serializable(with = InstantAsIsoStringSerializer::class)
    val occurredAt: Instant,
)

@Serializable
enum class ManualTagAssignmentStatus { APPLIED, DUPLICATE }

@Serializable
data class ManualTagAssignmentResponse(
    val eventId: String,
    val status: ManualTagAssignmentStatus,
    val assignedTagIds: Set<String>,
    val removedTagIds: Set<String>,
    val assignmentVersion: Long,
)
