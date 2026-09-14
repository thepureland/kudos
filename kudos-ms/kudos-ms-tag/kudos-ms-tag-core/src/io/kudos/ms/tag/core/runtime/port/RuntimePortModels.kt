package io.kudos.ms.tag.core.runtime.port

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import java.time.Instant

data class AttributeApplyResult(
    val applied: Boolean,
    val stateVersion: Long,
    val values: List<TagAttributeValue>,
)

data class TagEvaluationContext(
    val subjectKey: TagSubjectKey,
    val attributes: Map<String, List<TagAttributeValue>>,
    val activeTagCodes: Set<String>,
)

data class TagMembershipView(
    val id: String,
    val tagId: String,
    val source: TagMembershipSource,
    val sourceRef: String,
    val ruleVersion: Long?,
    val membershipVersion: Long,
    val effectiveFrom: Instant?,
    val effectiveUntil: Instant?,
)

data class ResolvedAssignment(
    val tagId: String,
    val tagSetId: String?,
    val assignmentVersion: Long,
    val evaluatedRuleVersion: Long?,
    val effectiveFrom: Instant? = null,
    val effectiveUntil: Instant? = null,
)

data class AssignmentDelta(
    val assignedTagIds: Set<String>,
    val removedTagIds: Set<String>,
    val assignmentVersion: Long,
)

data class TagKeysetPage(
    val afterSubjectId: String? = null,
    val size: Int = 100,
) {
    init {
        require(size in 1..1000) { "Tag query page size must be between 1 and 1000." }
    }
}

data class AssignmentSearchResult(
    val subjectIds: List<String>,
    val nextSubjectId: String?,
)

enum class RecalculationJobType { SUBJECT_INCREMENTAL, RULE_FULL_REBUILD }

data class RecalculationRequest(
    val tenantId: String,
    val jobType: RecalculationJobType,
    val tagId: String,
    val ruleVersion: Long,
    val subjectType: String,
    val subjectId: String? = null,
    val priority: Int = 100,
    val requestedVersion: Long = 1,
)

data class LeasedRecalculationJob(
    val id: String,
    val tenantId: String,
    val jobType: RecalculationJobType,
    val tagId: String,
    val ruleVersion: Long,
    val subjectType: String,
    val subjectId: String?,
    val cursorSubjectId: String?,
    val requestedVersion: Long,
    val processedVersion: Long,
    val leaseOwner: String,
    val leaseUntil: Instant,
    val version: Long,
)
