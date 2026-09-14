package io.kudos.ms.tag.common.runtime.model

import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import kotlinx.serialization.Serializable

@Serializable
data class TagRecalculateSubjectsRequest(
    val subjectKeys: List<TagSubjectKey>,
    val directRuleLimit: Int = 200,
) {
    init {
        require(subjectKeys.isNotEmpty()) { "At least one subject key is required." }
        require(directRuleLimit in 1..10_000) { "Direct rule limit must be between 1 and 10000." }
    }
}

@Serializable
data class TagRecalculationResult(
    val subjectsProcessed: Int,
    val directRulesEvaluated: Int,
    val cascadedRulesEvaluated: Int,
    val assignmentChanges: Int,
)
